"""
访客数据生成服务

参照 Future_Pridect.py 中的景点特征配置系统，生成更加合理的访客数据。
考虑景点类型、时间段、周末/节假日、天气等多个维度的影响。
"""

import math
import random
from datetime import datetime
from typing import Any, Dict, List, Optional, Tuple

# ================================================
# 景点特征库（参照 Future_Pridect.py）
# ================================================

# 景点类型配置：基础流量范围、峰值权重、各类加成系数
PROFILE_LIBRARY = {
    "nightlife": {
        "base_range": (12000, 25000),
        "peak_weights": (0.18, 0.48, 1.08),  # morning, noon, evening
        "weekend_boost": 0.30,
        "holiday_boost": 0.34,
        "night_event_boost": 0.42,
        "rain_coef": -0.11,
        "temp_coef": 0.04,
        "noise_std": 0.06,
    },
    "commercial_core": {
        "base_range": (15000, 28000),
        "peak_weights": (0.24, 0.72, 0.92),
        "weekend_boost": 0.28,
        "holiday_boost": 0.33,
        "night_event_boost": 0.24,
        "rain_coef": -0.06,
        "temp_coef": 0.02,
        "noise_std": 0.06,
    },
    "transport": {
        "base_range": (8500, 16000),
        "peak_weights": (0.42, 0.82, 0.58),
        "weekend_boost": 0.26,
        "holiday_boost": 0.35,
        "night_event_boost": 0.16,
        "rain_coef": -0.13,
        "temp_coef": 0.03,
        "noise_std": 0.06,
    },
    "historic": {
        "base_range": (10000, 18000),
        "peak_weights": (0.26, 0.74, 0.68),
        "weekend_boost": 0.30,
        "holiday_boost": 0.36,
        "night_event_boost": 0.22,
        "rain_coef": -0.09,
        "temp_coef": 0.04,
        "noise_std": 0.06,
    },
    "museum": {
        "base_range": (6000, 12000),
        "peak_weights": (0.54, 0.80, 0.20),
        "weekend_boost": 0.19,
        "holiday_boost": 0.24,
        "night_event_boost": 0.06,
        "rain_coef": 0.03,
        "temp_coef": -0.01,
        "noise_std": 0.04,
    },
    "park": {
        "base_range": (5000, 10000),
        "peak_weights": (0.45, 0.64, 0.40),
        "weekend_boost": 0.28,
        "holiday_boost": 0.30,
        "night_event_boost": 0.10,
        "rain_coef": -0.18,
        "temp_coef": 0.06,
        "noise_std": 0.05,
    },
    "theater": {
        "base_range": (4500, 9500),
        "peak_weights": (0.08, 0.30, 1.00),
        "weekend_boost": 0.24,
        "holiday_boost": 0.30,
        "night_event_boost": 0.36,
        "rain_coef": -0.03,
        "temp_coef": 0.01,
        "noise_std": 0.05,
    },
    "mountain_remote": {
        "base_range": (2000, 6000),
        "peak_weights": (0.42, 0.65, 0.22),
        "weekend_boost": 0.36,
        "holiday_boost": 0.38,
        "night_event_boost": 0.04,
        "rain_coef": -0.21,
        "temp_coef": 0.05,
        "noise_std": 0.07,
    },
    "default": {
        "base_range": (7000, 14000),
        "peak_weights": (0.30, 0.58, 0.36),
        "weekend_boost": 0.22,
        "holiday_boost": 0.28,
        "night_event_boost": 0.12,
        "rain_coef": -0.10,
        "temp_coef": 0.03,
        "noise_std": 0.05,
    },
}

# 重庆著名景点的特征覆盖：(类型, 热度倍数)
SPOT_PROFILE_OVERRIDES = {
    "洪崖洞": ("nightlife", 1.30),
    "解放碑": ("commercial_core", 1.28),
    "长江索道": ("transport", 1.22),
    "朝天门广场": ("park", 1.08),
    "江北嘴公园": ("park", 0.98),
    "观音桥": ("commercial_core", 1.25),
    "大礼堂": ("museum", 1.05),
    "十八梯": ("historic", 1.15),
    "白公馆渣滓洞": ("museum", 1.00),
    "歌乐山革命纪念馆": ("museum", 0.95),
    "南山植物园（北门）": ("park", 1.05),
    "重庆大剧院": ("theater", 1.08),
    "磁器口": ("historic", 1.30),
    "建川博物馆": ("museum", 0.92),
    "鸿恩寺": ("park", 0.92),
    "鹅岭公园": ("park", 0.95),
    "鹅岭印刷二厂": ("historic", 1.02),
    "三峡博物馆": ("museum", 1.18),
    "龙门浩": ("nightlife", 1.05),
    "南滨路": ("nightlife", 1.22),
    "金刚碑": ("mountain_remote", 0.85),
}

# 重庆中心坐标（用于计算景点区位因子）
CHONGQING_CENTER = (29.5575, 106.5773)

# 仅对具备入园时段约束的景区生效，商圈/夜游默认全天。
PROFILE_OPEN_WINDOWS = {
    "museum": (9.0, 17.0),
    "historic": (8.0, 21.0),
    "transport": (8.0, 22.0),
    "theater": (10.0, 22.5),
    "mountain_remote": (8.0, 18.0),
    "park": None,
    "nightlife": None,
    "commercial_core": None,
    "default": None,
}

SPOT_OPEN_WINDOW_OVERRIDES = {
    "长江索道": (8.0, 22.0),
    "三峡博物馆": (9.0, 17.0),
    "建川博物馆": (9.0, 17.0),
    "白公馆渣滓洞": (9.0, 17.0),
    "歌乐山革命纪念馆": (9.0, 17.0),
    "南山植物园（北门）": (8.0, 18.0),
    "南山植物园": (8.0, 18.0),
    "金刚碑": (8.0, 18.0),
}

CATEGORY_PROFILE_HINTS = {
    "nightlife": ("night", "bar", "酒吧", "夜", "夜游", "夜市"),
    "commercial_core": ("commercial", "mall", "shopping", "商圈", "商业", "步行街"),
    "transport": ("transport", "交通", "索道", "码头", "车站", "渡口"),
    "historic": ("historic", "history", "古", "历史", "文化街"),
    "museum": ("museum", "博物馆", "纪念馆", "展览"),
    "park": ("park", "公园", "园", "广场"),
    "theater": ("theater", "theatre", "剧院", "演艺"),
    "mountain_remote": ("mountain", "山", "郊野", "森林"),
}

# 缓存每个景点的基础流量（任务初始化时建立，减少小时级跳变）
_spot_base_flow_cache: Dict[int, float] = {}
_spot_profile_cache: Dict[int, Dict[str, Any]] = {}

# 运行时状态：用于构建连续时序（AR(1)）
_spot_noise_state: Dict[int, float] = {}
_spot_spike_state: Dict[int, float] = {}
_last_hour_context: Dict[str, Any] = {
    "hour": None,
    "rain": 0.0,
    "temp": 18.0,
    "city_shock": 0.0,
}


# ================================================
# 辅助函数
# ================================================

def _circular_hour_gaussian(hour: float, peak: float, width: float = 2.5) -> float:
    """计算当前小时相对于峰值的高斯权重。
    
    Args:
        hour: 当前小时（0-23）
        peak: 峰值小时（如 10.0 表示 10:00）
        width: 高斯曲线宽度
        
    Returns:
        [0, 1] 范围内的权重
    """
    distance = abs(hour - peak)
    distance = min(distance, 24 - distance)  # 处理循环边界
    return math.exp(-0.5 * (distance / max(width, 1e-6)) ** 2)


def _haversine_km(lat1: float, lon1: float, lat2: float, lon2: float) -> float:
    phi1 = math.radians(lat1)
    phi2 = math.radians(lat2)
    d_phi = math.radians(lat2 - lat1)
    d_lam = math.radians(lon2 - lon1)
    a = math.sin(d_phi / 2.0) ** 2 + math.cos(phi1) * math.cos(phi2) * math.sin(d_lam / 2.0) ** 2
    return 6371.0 * (2.0 * math.atan2(math.sqrt(a), math.sqrt(max(1.0 - a, 1e-12))))


def _safe_float(value: Any, fallback: float) -> float:
    try:
        return float(value)
    except (TypeError, ValueError):
        return fallback


def _parse_hour_value(value: Any) -> Optional[float]:
    if value is None:
        return None
    if isinstance(value, (int, float)):
        hour = float(value)
        return hour if 0.0 <= hour <= 24.0 else None
    if isinstance(value, str):
        text = value.strip()
        if not text:
            return None
        if ":" in text:
            hh, mm = text.split(":", 1)
            try:
                hour = float(int(hh)) + float(int(mm)) / 60.0
            except ValueError:
                return None
            return hour if 0.0 <= hour <= 24.0 else None
        try:
            hour = float(text)
            return hour if 0.0 <= hour <= 24.0 else None
        except ValueError:
            return None
    return None


def _resolve_open_window(spot: Dict[str, Any], profile_key: str) -> Optional[Tuple[float, float]]:
    for start_key, end_key in [
        ("entryStartHour", "entryEndHour"),
        ("openHour", "closeHour"),
        ("open_hour", "close_hour"),
        ("openTime", "closeTime"),
        ("open_time", "close_time"),
        ("startHour", "endHour"),
        ("start_hour", "end_hour"),
    ]:
        start = _parse_hour_value(spot.get(start_key))
        end = _parse_hour_value(spot.get(end_key))
        if start is not None and end is not None:
            return start, end

    title = str(spot.get("title", ""))
    if title in SPOT_OPEN_WINDOW_OVERRIDES:
        return SPOT_OPEN_WINDOW_OVERRIDES[title]
    return PROFILE_OPEN_WINDOWS.get(profile_key)


def _build_open_gate(hour: float, window: Optional[Tuple[float, float]]) -> float:
    if window is None:
        return 1.0
    start, end = window
    if abs(start - end) < 1e-6:
        return 1.0
    if start < end:
        return 1.0 if start <= hour < end else 0.0
    return 1.0 if (hour >= start or hour < end) else 0.0


def _infer_profile_key(spot_title: str, category: Optional[str]) -> str:
    override = SPOT_PROFILE_OVERRIDES.get(spot_title)
    if override:
        return override[0]
    if not category:
        return "default"
    category_text = str(category).lower()
    for profile_key, keywords in CATEGORY_PROFILE_HINTS.items():
        if any(k.lower() in category_text for k in keywords):
            return profile_key
    return "default"


def _get_spot_profile(spot_title: str, category: Optional[str] = None) -> Dict:
    """根据景点名称获取特征配置。
    
    Args:
        spot_title: 景点名称
        
    Returns:
        包含 profile_key, hotspot, 及全量配置的字典
    """
    profile_key = _infer_profile_key(spot_title, category)
    hotspot = SPOT_PROFILE_OVERRIDES.get(spot_title, (profile_key, 1.0))[1]
    profile = dict(PROFILE_LIBRARY[profile_key])
    profile["hotspot"] = hotspot
    profile["profile_key"] = profile_key
    return profile


def _build_time_curve(hour: float, profile_key: str) -> float:
    """计算某个小时的相对访客系数（0~1+）。
    
    基础系数 0.52 + 三个峰值的加权高斯分布：
    - morning peak 在 10:00（权重取 peak_weights[0]）
    - noon peak 在 14:30（权重取 peak_weights[1]）
    - evening peak 在 20:30（权重取 peak_weights[2]）
    
    Args:
        hour: 当前小时（0-23）
        peak_weights: (morning_w, noon_w, evening_w)
        
    Returns:
        [0.52, ...] 范围的相对系数
    """
    if profile_key == "commercial_core":
        return (
            0.22
            + 0.95 * _circular_hour_gaussian(hour, 13.0, 2.3)
            + 1.05 * _circular_hour_gaussian(hour, 20.2, 2.6)
            + 0.25 * _circular_hour_gaussian(hour, 0.8, 2.2)
        )
    if profile_key == "nightlife":
        return (
            0.14
            + 0.40 * _circular_hour_gaussian(hour, 13.5, 3.0)
            + 1.20 * _circular_hour_gaussian(hour, 21.3, 2.4)
            + 0.38 * _circular_hour_gaussian(hour, 0.7, 2.1)
        )
    if profile_key == "park":
        return (
            0.01
            + 0.96 * _circular_hour_gaussian(hour, 10.8, 2.5)
            + 0.74 * _circular_hour_gaussian(hour, 16.2, 2.7)
            + 0.05 * _circular_hour_gaussian(hour, 19.2, 1.8)
        )
    if profile_key == "museum":
        return (
            0.05
            + 0.85 * _circular_hour_gaussian(hour, 10.3, 2.1)
            + 0.82 * _circular_hour_gaussian(hour, 14.2, 2.4)
            + 0.14 * _circular_hour_gaussian(hour, 18.0, 1.8)
        )
    if profile_key == "historic":
        return (
            0.06
            + 0.68 * _circular_hour_gaussian(hour, 10.0, 2.4)
            + 0.92 * _circular_hour_gaussian(hour, 15.2, 2.7)
            + 0.30 * _circular_hour_gaussian(hour, 19.0, 2.3)
        )
    if profile_key == "transport":
        return (
            0.12
            + 0.78 * _circular_hour_gaussian(hour, 9.2, 2.0)
            + 0.88 * _circular_hour_gaussian(hour, 15.8, 2.4)
            + 0.42 * _circular_hour_gaussian(hour, 19.2, 2.2)
        )
    if profile_key == "mountain_remote":
        return (
            0.03
            + 0.88 * _circular_hour_gaussian(hour, 10.6, 2.6)
            + 0.64 * _circular_hour_gaussian(hour, 14.8, 2.9)
            + 0.10 * _circular_hour_gaussian(hour, 18.0, 2.0)
        )
    if profile_key == "theater":
        return (
            0.08
            + 0.30 * _circular_hour_gaussian(hour, 14.0, 2.5)
            + 1.05 * _circular_hour_gaussian(hour, 20.5, 2.2)
            + 0.18 * _circular_hour_gaussian(hour, 23.0, 1.6)
        )
    return (
        0.08
        + 0.82 * _circular_hour_gaussian(hour, 10.0, 2.5)
        + 0.96 * _circular_hour_gaussian(hour, 15.0, 2.8)
        + 0.32 * _circular_hour_gaussian(hour, 19.0, 2.2)
    )


def _get_day_modifiers(dt: datetime) -> Tuple[float, float, bool]:
    """获取当前日期的各类加成因子。
    
    Args:
        dt: 当前时间
        
    Returns:
        (is_weekend, is_holiday, is_friday_night)
    """
    day_of_week = dt.weekday()  # 0=Monday, 6=Sunday
    is_weekend = 1.0 if day_of_week >= 5 else 0.0
    
    hour = dt.hour
    is_friday_night = bool(day_of_week >= 4 and 19 <= hour <= 23)
    
    # 简化节假日处理：使用日期的 hash 值确定是否节假日
    # 约 22% 的日期被标记为节假日
    day_hash = (dt.year * 10000 + dt.month * 100 + dt.day) % 100
    is_holiday = 1.0 if day_hash < 22 else 0.0
    
    return is_weekend, is_holiday, is_friday_night


def _next_city_context(current_time: datetime) -> Dict[str, float]:
    hour_key = current_time.replace(minute=0, second=0, microsecond=0)
    if _last_hour_context["hour"] == hour_key:
        return {
            "rain": float(_last_hour_context["rain"]),
            "temp": float(_last_hour_context["temp"]),
            "city_shock": float(_last_hour_context["city_shock"]),
        }

    day_of_year = current_time.timetuple().tm_yday
    season_phase = (day_of_year / 365.0) * 1.5 * math.pi
    temp = 14.0 + 7.0 * math.sin(season_phase) + random.gauss(0.0, 1.8)
    rain = random.betavariate(2.0, 5.0)

    raw_city_noise = random.gauss(0.0, 0.05)
    prev_city_shock = float(_last_hour_context["city_shock"])
    hour_of_week = (current_time.weekday() * 24 + current_time.hour) % 168
    trend = 0.03 * math.sin((2.0 * math.pi * hour_of_week) / 168.0)
    city_shock = 0.85 * prev_city_shock + 0.15 * raw_city_noise + trend

    _last_hour_context["hour"] = hour_key
    _last_hour_context["rain"] = rain
    _last_hour_context["temp"] = temp
    _last_hour_context["city_shock"] = city_shock
    return {"rain": rain, "temp": temp, "city_shock": city_shock}


def _next_spot_ar1_noise(spot_id: int, noise_std: float, phi: float = 0.70) -> float:
    prev = _spot_noise_state.get(spot_id, 0.0)
    raw = random.gauss(0.0, noise_std)
    noise = phi * prev + (1.0 - phi) * raw
    _spot_noise_state[spot_id] = noise
    return noise


def _next_spot_spike(spot_id: int, spike_scale: float, event_prob: float = 1.0 / 72.0) -> float:
    prev = _spot_spike_state.get(spot_id, 0.0)
    impulse = random.uniform(0.05, spike_scale) if random.random() < event_prob else 0.0
    spike = 0.82 * prev + impulse
    _spot_spike_state[spot_id] = spike
    return spike


def _compute_weather_effect(profile: Dict[str, Any], rain: float, temp: float) -> float:
    temp_norm = (temp - 18.0) / 10.0
    return max(0.65, min(1.35, 1.0 + profile["rain_coef"] * rain + profile["temp_coef"] * temp_norm))


def reset_runtime_state() -> None:
    """重置运行时状态（AR(1) 噪声、城市扰动、突发事件），用于批量历史生成前归一化。"""
    _spot_noise_state.clear()
    _spot_spike_state.clear()
    _last_hour_context["hour"] = None
    _last_hour_context["rain"] = 0.0
    _last_hour_context["temp"] = 18.0
    _last_hour_context["city_shock"] = 0.0


def initialize_spot_profiles(spots: List[Dict], reset_state: bool = True) -> None:
    """为景点初始化基础流量缓存。
    
    在启动模拟任务时调用，为每个景点生成一个基础流量值（保持在该景点整个运行期间的稳定性）。
    
    Args:
        spots: 景点列表（每个元素包含 'id', 'title', 'latitude', 'longitude'）
    """
    global _spot_base_flow_cache
    _spot_base_flow_cache.clear()
    _spot_profile_cache.clear()

    if reset_state:
        reset_runtime_state()
    
    for spot in spots:
        spot_id = int(spot["id"])
        spot_title = str(spot.get("title", ""))
        profile = _get_spot_profile(spot_title, str(spot.get("category", "")))

        lat = _safe_float(spot.get("latitude"), CHONGQING_CENTER[0])
        lon = _safe_float(spot.get("longitude"), CHONGQING_CENTER[1])
        dist_center = _haversine_km(lat, lon, CHONGQING_CENTER[0], CHONGQING_CENTER[1])
        urban_factor = 0.72 + 0.35 * math.exp(-dist_center / 12.0)

        base_low, base_high = profile["base_range"]
        base_flow = random.uniform(base_low, base_high) * urban_factor * profile["hotspot"]
        _spot_base_flow_cache[spot_id] = max(200.0, base_flow)
        _spot_profile_cache[spot_id] = profile


def _ensure_spot_profiles(spots: List[Dict]) -> None:
    missing = [s for s in spots if int(s["id"]) not in _spot_base_flow_cache]
    if not missing:
        return
    for spot in missing:
        spot_id = int(spot["id"])
        spot_title = str(spot.get("title", ""))
        profile = _get_spot_profile(spot_title, str(spot.get("category", "")))

        lat = _safe_float(spot.get("latitude"), CHONGQING_CENTER[0])
        lon = _safe_float(spot.get("longitude"), CHONGQING_CENTER[1])
        dist_center = _haversine_km(lat, lon, CHONGQING_CENTER[0], CHONGQING_CENTER[1])
        urban_factor = 0.72 + 0.35 * math.exp(-dist_center / 12.0)

        base_low, base_high = profile["base_range"]
        base_flow = random.uniform(base_low, base_high) * urban_factor * profile["hotspot"]
        _spot_base_flow_cache[spot_id] = max(200.0, base_flow)
        _spot_profile_cache[spot_id] = profile


def generate_visitor_count(
    spot_id: int,
    spot_title: str,
    current_time: datetime,
) -> int:
    """生成某个景点在某个时刻的访客数。
    
    综合考虑：
    1. 景点类型和基础流量
    2. 当前小时的时间段权重（morning/noon/evening 峰值）
    3. 是否周末、是否节假日
    4. 随机天气因素
    5. 噪声和随机波动
    
    Args:
        spot_id: 景点 ID
        spot_title: 景点名称
        current_time: 当前时间（用于确定小时、周末、节假日等）
        
    Returns:
        访客数（整数，最小 300，最大根据景点类型确定）
    """
    if spot_id not in _spot_base_flow_cache:
        initialize_spot_profiles([{"id": spot_id, "title": spot_title}], reset_state=False)

    profile = _spot_profile_cache.get(spot_id)
    if profile is None:
        profile = _get_spot_profile(spot_title)
        _spot_profile_cache[spot_id] = profile

    base_flow = _spot_base_flow_cache[spot_id]
    hour_context = _next_city_context(current_time)

    hour = float(current_time.hour)
    time_curve = _build_time_curve(hour, profile["profile_key"])
    open_gate = _build_open_gate(hour, _resolve_open_window({"title": spot_title}, profile["profile_key"]))

    is_weekend, is_holiday, is_friday_night = _get_day_modifiers(current_time)
    weekend_effect = 1.0 + profile["weekend_boost"] * is_weekend
    holiday_effect = 1.0 + profile["holiday_boost"] * is_holiday
    night_effect = 1.0 + profile["night_event_boost"] * (1.0 if is_friday_night else 0.0)
    weather_effect = _compute_weather_effect(profile, hour_context["rain"], hour_context["temp"])

    noise = _next_spot_ar1_noise(spot_id, profile["noise_std"], phi=0.70)
    spike = _next_spot_spike(spot_id, profile.get("spike_scale", 0.16))

    flow = base_flow * time_curve * open_gate * weekend_effect * holiday_effect * night_effect * weather_effect
    flow *= max(0.0, 1.0 + hour_context["city_shock"] + noise + spike)
    flow = max(0.0, min(max(base_flow * 4.5, 2000.0), flow))
    return int(round(flow))


def generate_all_visitor_counts(
    spots: List[Dict],
    current_time: Optional[datetime] = None,
) -> Dict[int, int]:
    """为所有景点生成当前时刻的访客数。
    
    Args:
        spots: 景点列表
        current_time: 当前时间（如果为 None，使用 datetime.now()）
        
    Returns:
        {spot_id: visitor_count} 字典
    """
    if current_time is None:
        current_time = datetime.now()

    _ensure_spot_profiles(spots)
    hour_context = _next_city_context(current_time)
    
    result = {}
    for spot in spots:
        spot_id = int(spot["id"])
        spot_title = str(spot.get("title", ""))
        profile = _spot_profile_cache.get(spot_id)
        if profile is None:
            profile = _get_spot_profile(spot_title, str(spot.get("category", "")))
            _spot_profile_cache[spot_id] = profile

        base_flow = _spot_base_flow_cache[spot_id]
        hour = float(current_time.hour)
        open_gate = _build_open_gate(hour, _resolve_open_window(spot, profile["profile_key"]))
        time_curve = _build_time_curve(hour, profile["profile_key"])

        is_weekend, is_holiday, is_friday_night = _get_day_modifiers(current_time)
        weekend_effect = 1.0 + profile["weekend_boost"] * is_weekend
        holiday_effect = 1.0 + profile["holiday_boost"] * is_holiday
        night_effect = 1.0 + profile["night_event_boost"] * (1.0 if is_friday_night else 0.0)
        weather_effect = _compute_weather_effect(profile, hour_context["rain"], hour_context["temp"])

        noise = _next_spot_ar1_noise(spot_id, profile["noise_std"], phi=0.70)
        spike = _next_spot_spike(spot_id, profile.get("spike_scale", 0.16))

        flow = base_flow * time_curve * open_gate * weekend_effect * holiday_effect * night_effect * weather_effect
        flow *= max(0.0, 1.0 + hour_context["city_shock"] + noise + spike)
        flow = max(0.0, min(max(base_flow * 4.5, 2000.0), flow))

        visitor_count = int(round(flow))
        result[spot_id] = visitor_count
    
    return result
