"""
景区拥挤指数模型实现。

模型步骤（对应报告）：
- F1：有效承载力：天气/管控折减 + 空间/区域系数。
- F2：负载比 r_i = N_i / C_i_eff。
- F3：分段函数 Phi(r) 负载到拥挤映射。
- F4/F5：服务区排队修正项。
- F6：景区聚合：加权平均 + 关键区域峰值约束。
"""

from __future__ import annotations

from dataclasses import dataclass
import math
from typing import Iterable, Mapping, Optional, Sequence


@dataclass(frozen=True)
class ZoneInput:
	"""单个区域在当前时间步的输入数据。"""

	zone_id: str
	zone_type: str
	max_capacity: float
	people_now: float
	wait_minutes: Optional[float] = None
	space_type: Optional[str] = None


# 天气等级映射 -> eta_weather（F1）。
DEFAULT_WEATHER_COEFF: Mapping[str, float] = {
	"sunny": 1.0,
	"cloudy": 1.0,
	"light_rain": 0.85,
	"moderate_rain": 0.70,
	"heavy_rain": 0.50,
}

# 管控状态映射 -> eta_manage（F1）。
DEFAULT_MANAGE_COEFF: Mapping[str, float] = {
	"normal": 1.0,
	"restricted": 0.85,
	"closed": 0.60,
	"construction": 0.60,
}

# 可选空间类型系数；缺失时回退到区域类型系数。
DEFAULT_SPACE_TYPE_KAPPA: Mapping[str, float] = {
	"OPEN": 1.0,
	"SEMI_CLOSED": 0.8,
	"CLOSED": 0.7,
}

# 区域类型系数，用于有效承载力 F1。
DEFAULT_ZONE_TYPE_KAPPA: Mapping[str, float] = {
	"CORE": 0.8,
	"RIDE": 0.8,
	"TOILET": 0.7,
	"DINING": 0.8,
	"SHOP": 0.8,
	"EXHIBIT": 0.7,
	"ENTRY": 1.0,
	"EXIT": 1.0,
	"PATH": 1.0,
	"TRANSIT": 1.0,
	"PARKING": 1.0,
}

# 区域类型权重，用于景区聚合 F6。
DEFAULT_ZONE_TYPE_WEIGHT: Mapping[str, float] = {
	"CORE": 1.50,
	"RIDE": 1.40,
	"TOILET": 1.30,
	"ENTRY": 1.20,
	"EXIT": 1.10,
	"EXHIBIT": 1.10,
	"DINING": 1.00,
	"SHOP": 0.90,
	"PATH": 0.80,
	"TRANSIT": 1.00,
	"PARKING": 0.75,
}

# 服务型区域启用排队修正（F4/F5）。
DEFAULT_SERVICE_ZONE_TYPES = {"ENTRY", "RIDE", "TOILET", "DINING"}
# 关键区域参与峰值约束（F6）。
DEFAULT_PEAK_ZONE_TYPES = {"CORE", "ENTRY", "RIDE"}


def _normalize_zone_type(value: str) -> str:
	# 统一为大写键值。
	return value.strip().upper()


def _normalize_state(value: str) -> str:
	# 统一为小写键值。
	return value.strip().lower()


def _get_coeff(
	mapping: Mapping[str, float],
	key: str,
	*,
	default: Optional[float] = None,
	strict: bool = True,
	normalize,
	label: str,
) -> float:
	"""从映射表中读取系数，可选择严格校验。"""

	if key is None:
		if strict:
			raise ValueError(f"Missing {label} value")
		if default is None:
			raise ValueError(f"Missing {label} value and no default")
		return default

	normalized = normalize(key)
	if normalized in mapping:
		return float(mapping[normalized])

	if strict:
		raise ValueError(f"Unknown {label} value: {key}")
	if default is None:
		raise ValueError(f"Unknown {label} value and no default: {key}")
	return default


def _phi(ratio: float, r1: float, r2: float) -> float:
	"""分段函数 Phi(r)：低载线性、中载快速上升、高载饱和。"""

	if ratio < 0:
		raise ValueError("ratio must be non-negative")
	if ratio < r1:
		return 0.5 * ratio
	if ratio < r2:
		return 0.3 + 1.5 * (ratio - r1)
	return 0.6 + 0.4 * (1.0 - math.exp(-4.0 * (ratio - r2)))


def _queue_factor(wait_minutes: Optional[float]) -> float:
	"""排队修正项 Q_i = min(1, wait_minutes / 30)。"""

	if wait_minutes is None:
		return 0.0
	if wait_minutes < 0:
		raise ValueError("wait_minutes must be non-negative")
	return min(1.0, float(wait_minutes) / 30.0)


def compute_zone_crowd_index(
	zone: ZoneInput,
	*,
	eta_weather: float,
	eta_manage: float,
	r1: float,
	r2: float,
	zone_type_kappa: Mapping[str, float],
	space_type_kappa: Mapping[str, float],
	service_zone_types: Sequence[str],
	strict: bool,
) -> float:
	"""计算单区域拥挤指数 CI_i，包含有效承载力与排队修正。"""

	if zone.max_capacity <= 0:
		raise ValueError(f"Invalid max_capacity for zone {zone.zone_id}")
	if zone.people_now < 0:
		raise ValueError(f"Invalid people_now for zone {zone.zone_id}")

	zone_type = _normalize_zone_type(zone.zone_type)
	kappa_mapping = space_type_kappa if zone.space_type else zone_type_kappa
	kappa_key = zone.space_type if zone.space_type else zone_type
	# 若提供 space_type，则优先用空间类型系数，否则回退到区域类型系数。
	kappa = _get_coeff(
		kappa_mapping,
		kappa_key,
		default=1.0,
		strict=strict,
		normalize=_normalize_zone_type,
		label="kappa mapping",
	)

	# F1：C_i_eff = C_i_max * kappa_i * eta_weather * eta_manage。
	effective_capacity = zone.max_capacity * kappa * eta_weather * eta_manage
	if effective_capacity <= 0:
		raise ValueError(f"Invalid effective capacity for zone {zone.zone_id}")

	# F2：r_i = N_i / C_i_eff
	ratio = zone.people_now / effective_capacity
	# F3：Phi(r) 将负载比映射到基础拥挤度。
	base = _phi(ratio, r1, r2)
	if zone_type in set(_normalize_zone_type(t) for t in service_zone_types):
		# F4/F5：服务型区域叠加排队修正。
		queue = _queue_factor(zone.wait_minutes)
		return min(1.0, 0.85 * base + 0.15 * queue)
	return min(1.0, base)


def compute_scenic_crowd_index(
	zones: Iterable[ZoneInput],
	*,
	weather_level: str,
	manage_state: str,
	r1: float = 0.6,
	r2: float = 0.8,
	xi: float = 0.7,
	weather_coeff: Mapping[str, float] = DEFAULT_WEATHER_COEFF,
	manage_coeff: Mapping[str, float] = DEFAULT_MANAGE_COEFF,
	zone_type_kappa: Mapping[str, float] = DEFAULT_ZONE_TYPE_KAPPA,
	space_type_kappa: Mapping[str, float] = DEFAULT_SPACE_TYPE_KAPPA,
	zone_type_weight: Mapping[str, float] = DEFAULT_ZONE_TYPE_WEIGHT,
	service_zone_types: Sequence[str] = tuple(DEFAULT_SERVICE_ZONE_TYPES),
	peak_zone_types: Sequence[str] = tuple(DEFAULT_PEAK_ZONE_TYPES),
	strict: bool = True,
) -> float:
	"""
	计算当前时刻景区拥挤指数 SCI_s。

	公式：
	SCI_s = min(1, xi * SCI_base + (1 - xi) * SCI_peak)
	"""

	# 当前时刻景区级折减系数（F1）。
	eta_weather = _get_coeff(
		weather_coeff,
		weather_level,
		default=1.0,
		strict=strict,
		normalize=_normalize_state,
		label="weather_level",
	)
	eta_manage = _get_coeff(
		manage_coeff,
		manage_state,
		default=1.0,
		strict=strict,
		normalize=_normalize_state,
		label="manage_state",
	)

	weighted_sum = 0.0
	weight_total = 0.0
	peak_value = 0.0

	normalized_peak = {_normalize_zone_type(t) for t in peak_zone_types}
	for zone in zones:
		zone_type = _normalize_zone_type(zone.zone_type)
		weight = _get_coeff(
			zone_type_weight,
			zone_type,
			default=1.0,
			strict=strict,
			normalize=_normalize_zone_type,
			label="zone_type_weight",
		)
		ci = compute_zone_crowd_index(
			zone,
			eta_weather=eta_weather,
			eta_manage=eta_manage,
			r1=r1,
			r2=r2,
			zone_type_kappa=zone_type_kappa,
			space_type_kappa=space_type_kappa,
			service_zone_types=service_zone_types,
			strict=strict,
		)

		weighted_sum += weight * ci
		weight_total += weight
		# 关键区域峰值约束（如 CORE/ENTRY/RIDE）（F6）。
		if zone_type in normalized_peak:
			peak_value = max(peak_value, ci)

	if weight_total <= 0:
		raise ValueError("Total weight must be positive")

	# SCI_base = sum(w_i * CI_i) / sum(w_i)
	base = weighted_sum / weight_total
	# F6：SCI = min(1, xi * SCI_base + (1 - xi) * SCI_peak)
	scenic_index = min(1.0, xi * base + (1.0 - xi) * peak_value)
	return scenic_index


def example_xiannvshan_crowd_index() -> float:
	"""仙女山示例：用一组假设数据计算当前拥挤指数。"""

	zones = [
		ZoneInput(
			zone_id="ENTRY_EAST",
			zone_type="ENTRY",
			max_capacity=1500,
			people_now=1400,
			wait_minutes=25,
		),
		ZoneInput(
			zone_id="CORE_VIEW",
			zone_type="CORE",
			max_capacity=3000,
			people_now=2600,
		),
		ZoneInput(
			zone_id="RIDE_A",
			zone_type="RIDE",
			max_capacity=800,
			people_now=720,
			wait_minutes=40,
		),
		ZoneInput(
			zone_id="TOILET_A",
			zone_type="TOILET",
			max_capacity=200,
			people_now=140,
			wait_minutes=10,
		),
		ZoneInput(
			zone_id="PATH_MAIN",
			zone_type="PATH",
			max_capacity=5000,
			people_now=3000,
		),
	]

	return compute_scenic_crowd_index(
		zones,
		weather_level="sunny",
		manage_state="normal",
	)


def test_xiannvshan_crowd_index() -> None:
	"""仙女山测试：计算拥挤指数并做近似校验。"""

	index_value = example_xiannvshan_crowd_index()
	# 期望值约 0.80，允许小幅浮动。
	assert abs(index_value - 0.805) < 0.02


if __name__ == "__main__":
    test_xiannvshan_crowd_index()
    print(f"Example Xiannvshan Crowd Index: {example_xiannvshan_crowd_index():.3f}")8