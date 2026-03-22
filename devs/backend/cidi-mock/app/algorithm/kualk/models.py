"""
景区游客拥挤指数数学模型 - 核心数据结构定义

本模块定义了模型所需的所有数据结构，包括：
- 区域类型枚举
- 区域属性
- 通道属性
- 景区拓扑
- 时间序列数据
"""

from dataclasses import dataclass, field
from enum import Enum
from typing import Dict, List, Optional, Tuple
import numpy as np


class ZoneType(Enum):
    """区域功能类型枚举"""
    ENTRY = "ENTRY"           # 入口/检票区
    EXIT = "EXIT"             # 出口/返程通道
    PATH = "PATH"             # 通行通道
    CORE = "CORE"             # 核心景点
    RIDE = "RIDE"             # 游乐/体验项目
    EXHIBIT = "EXHIBIT"       # 展陈区
    DINING = "DINING"         # 餐饮/休息区
    TOILET = "TOILET"         # 卫生间
    SHOP = "SHOP"             # 购物/文创区
    TRANSIT = "TRANSIT"       # 接驳交通区
    PARKING = "PARKING"       # 停车/换乘区


class SpaceForm(Enum):
    """空间形态类型"""
    ENCLOSED = "enclosed"     # 全封闭室内
    SEMI_ENCLOSED = "semi"    # 半封闭空间
    OPEN = "open"             # 开敞室外空间


class WeatherType(Enum):
    """天气类型"""
    SUNNY = "sunny"           # 晴天/多云
    LIGHT_RAIN = "light_rain" # 小雨/微风
    MODERATE_RAIN = "moderate_rain"  # 中雨/高温(>38°C)
    SEVERE = "severe"         # 暴雨/暴雪/极端天气


class CrowdingLevel(Enum):
    """拥挤等级"""
    COMFORTABLE = "I"         # 舒适 [0, 0.25)
    MODERATE = "II"           # 一般 [0.25, 0.50)
    CROWDED = "III"           # 拥挤 [0.50, 0.70)
    VERY_CROWDED = "IV"       # 非常拥挤 [0.70, 0.85)
    EXTREME = "V"             # 极度拥挤/预警 [0.85, 1.0]


@dataclass
class Zone:
    """
    功能区域属性元组
    
    z_i = (S_i, C_i^max, τ_i, κ_i, μ_i, s_i)
    """
    zone_id: str                          # 区域唯一标识
    name: str                             # 区域名称
    zone_type: ZoneType                   # 区域功能类型
    area: float                           # 有效可活动面积 (㎡)
    max_density: float = 2.5              # 最大舒适密度 (人/㎡)，默认值
    service_rate: float = 0.0             # 服务速率 (人/分钟/窗口)，仅服务类区域
    service_windows: int = 0              # 服务窗口/通道数，仅服务类区域
    space_form: SpaceForm = SpaceForm.OPEN  # 空间形态
    
    def __post_init__(self):
        """计算派生属性"""
        self._kappa = self._compute_kappa()
        self._max_capacity = self.area * self.max_density
    
    def _compute_kappa(self) -> float:
        """计算空间形态系数"""
        kappa_map = {
            SpaceForm.ENCLOSED: 0.60,      # 0.50 ~ 0.70
            SpaceForm.SEMI_ENCLOSED: 0.775, # 0.70 ~ 0.85
            SpaceForm.OPEN: 0.925           # 0.85 ~ 1.00
        }
        return kappa_map[self.space_form]
    
    @property
    def kappa(self) -> float:
        """空间形态系数"""
        return self._kappa
    
    @property
    def max_capacity(self) -> float:
        """设计最大承载量"""
        return self._max_capacity
    
    @property
    def is_service_zone(self) -> bool:
        """是否为服务类区域（具有排队特征）"""
        return self.zone_type in {
            ZoneType.ENTRY, ZoneType.EXIT, ZoneType.RIDE,
            ZoneType.TOILET, ZoneType.DINING, ZoneType.TRANSIT
        }


@dataclass
class Corridor:
    """
    通道属性
    
    e_ij = (d_ij, W_ij, Q_ij^max)
    """
    corridor_id: str                      # 通道唯一标识
    from_zone: str                        # 起始区域ID
    to_zone: str                          # 目标区域ID
    length: float                         # 通道长度 (米)
    width: float                          # 通道有效宽度 (米)
    free_speed: float = 1.3               # 自由行走速度 (m/s)
    jam_density: float = 6.0              # 完全拥堵密度 (人/㎡)
    speed_decay_exp: float = 1.75         # 速度衰减指数
    
    def __post_init__(self):
        """计算派生属性"""
        self._max_flow_rate = self._compute_max_flow_rate()
    
    def _compute_max_flow_rate(self) -> float:
        """计算通道最大通行流量 (人/分钟)"""
        max_flow_density = 1.25  # 最大通行密度 (人/㎡)
        return self.width * self.free_speed * max_flow_density * 60  # 转换为人/分钟
    
    @property
    def max_flow_rate(self) -> float:
        """通道最大通行流量 (人/分钟)"""
        return self._max_flow_rate
    
    def compute_speed(self, density: float) -> float:
        """
        根据密度计算行人速度 (Greenshields模型改进)
        
        v_ij(t) = v_free * (1 - ρ_ij(t)/ρ_jam)^α
        """
        if density >= self.jam_density:
            return 0.0
        ratio = density / self.jam_density
        return self.free_speed * ((1 - ratio) ** self.speed_decay_exp)
    
    def compute_actual_capacity(self, density: float) -> float:
        """
        计算通道实际通行能力 (人/分钟)
        
        Q_ij^max(t) = W_ij * v_ij(t) * ρ_ij(t)
        """
        speed = self.compute_speed(density)
        return self.width * speed * density * 60  # 转换为人/分钟


@dataclass
class ScenicArea:
    """
    景区拓扑结构
    
    G^(s) = (Z^(s), E^(s))
    """
    scenic_id: str                        # 景区唯一标识
    name: str                             # 景区名称
    zones: Dict[str, Zone] = field(default_factory=dict)      # 区域字典
    corridors: Dict[str, Corridor] = field(default_factory=dict)  # 通道字典
    
    def add_zone(self, zone: Zone) -> None:
        """添加区域"""
        self.zones[zone.zone_id] = zone
    
    def add_corridor(self, corridor: Corridor) -> None:
        """添加通道"""
        self.corridors[corridor.corridor_id] = corridor
    
    def get_outbound_corridors(self, zone_id: str) -> List[Corridor]:
        """获取从指定区域出发的所有通道"""
        return [c for c in self.corridors.values() if c.from_zone == zone_id]
    
    def get_inbound_corridors(self, zone_id: str) -> List[Corridor]:
        """获取到达指定区域的所有通道"""
        return [c for c in self.corridors.values() if c.to_zone == zone_id]
    
    def get_upstream_zones(self, zone_id: str) -> List[str]:
        """获取上游邻接区域ID列表"""
        return [c.from_zone for c in self.get_inbound_corridors(zone_id)]
    
    def get_downstream_zones(self, zone_id: str) -> List[str]:
        """获取下游邻接区域ID列表"""
        return [c.to_zone for c in self.get_outbound_corridors(zone_id)]


@dataclass
class ZoneState:
    """
    区域实时状态
    """
    zone_id: str
    timestamp: float                      # 时间戳 (分钟)
    visitor_count: float                  # 实时游客数量
    queue_count: float = 0.0              # 排队人数
    weather_factor: float = 1.0           # 天气折减系数
    event_factor: float = 1.0             # 临时活动占用折减
    facility_factor: float = 1.0          # 设施运行状态折减
    light_factor: float = 1.0             # 照明/能见度折减
    
    def get_total_reduction_factor(self) -> float:
        """计算总折减系数"""
        return self.weather_factor * self.event_factor * self.facility_factor * self.light_factor


@dataclass
class ZoneCrowdingResult:
    """
    区域拥挤指数计算结果
    """
    zone_id: str
    timestamp: float
    visitor_count: float                  # N_i(t)
    effective_capacity: float             # C_i^eff(t)
    load_ratio: float                     # r_i(t)
    base_crowding: float                  # Φ(r_i)
    bottleneck_factor: float              # B_i
    queue_contribution: float             # Ψ(W_i)
    propagation_contribution: float       # ΔCI_i^prop
    crowding_index: float                 # CI_i(t)
    crowding_level: CrowdingLevel         # 拥挤等级


@dataclass
class ScenicCrowdingResult:
    """
    景区拥挤指数计算结果
    """
    scenic_id: str
    timestamp: float
    zone_results: Dict[str, ZoneCrowdingResult] = field(default_factory=dict)
    base_index: float = 0.0               # SCI_s^base
    peak_index: float = 0.0               # SCI_s^peak
    raw_index: float = 0.0                # SCI_s^raw
    coupling_factor: float = 1.0          # Γ_s
    crowding_index: float = 0.0           # SCI_s
    crowding_level: CrowdingLevel = CrowdingLevel.COMFORTABLE
    
    @property
    def high_load_zone_count(self) -> int:
        """高负载区域数量"""
        return sum(1 for r in self.zone_results.values() if r.crowding_index > 0.6)
    
    @property
    def total_zones(self) -> int:
        """总区域数"""
        return len(self.zone_results)


@dataclass
class MultiScenicState:
    """
    多景区联动状态
    """
    timestamp: float
    scenic_indices: Dict[str, float] = field(default_factory=dict)      # 各景区拥挤指数
    remaining_capacities: Dict[str, float] = field(default_factory=dict)  # 各景区剩余容量
    spill_over_flows: Dict[Tuple[str, str], float] = field(default_factory=dict)  # 景区间溢出客流


def get_crowding_level(ci: float) -> CrowdingLevel:
    """
    根据拥挤指数获取拥挤等级
    
    Args:
        ci: 拥挤指数 [0, 1]
    
    Returns:
        拥挤等级枚举值
    """
    if ci < 0.25:
        return CrowdingLevel.COMFORTABLE
    elif ci < 0.50:
        return CrowdingLevel.MODERATE
    elif ci < 0.70:
        return CrowdingLevel.CROWDED
    elif ci < 0.85:
        return CrowdingLevel.VERY_CROWDED
    else:
        return CrowdingLevel.EXTREME


def get_perception_weight(zone_type: ZoneType) -> float:
    """
    获取区域类型的感知权重
    
    Args:
        zone_type: 区域功能类型
    
    Returns:
        感知权重
    """
    weight_map = {
        ZoneType.CORE: 1.50,
        ZoneType.RIDE: 1.40,
        ZoneType.TOILET: 1.30,
        ZoneType.ENTRY: 1.20,
        ZoneType.EXIT: 1.15,
        ZoneType.EXHIBIT: 1.10,
        ZoneType.TRANSIT: 1.10,
        ZoneType.DINING: 1.00,
        ZoneType.SHOP: 0.90,
        ZoneType.PATH: 0.80,
        ZoneType.PARKING: 0.75,
    }
    return weight_map.get(zone_type, 1.0)


def get_weather_factor(weather: WeatherType) -> float:
    """
    获取天气折减系数
    
    Args:
        weather: 天气类型
    
    Returns:
        天气折减系数
    """
    factor_map = {
        WeatherType.SUNNY: 1.0,
        WeatherType.LIGHT_RAIN: 0.80,
        WeatherType.MODERATE_RAIN: 0.55,
        WeatherType.SEVERE: 0.35,
    }
    return factor_map.get(weather, 1.0)
