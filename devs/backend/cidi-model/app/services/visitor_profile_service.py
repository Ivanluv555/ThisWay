"""
访客数据生成服务

参照 Future_Pridect.py 中的景点特征配置系统，生成更加合理的访客数据。
考虑景点类型、时间段、周末/节假日、天气等多个维度的影响。
"""

import math
import random
from datetime import datetime
from typing import Dict, List, Optional, Tuple

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

# 缓存每个景点的基础流量（单实例任务启动时初始化）
_spot_base_flow_cache: Dict[int, float] = {}


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


def _get_spot_profile(spot_title: str) -> Dict:
    """根据景点名称获取特征配置。
    
    Args:
        spot_title: 景点名称
        
    Returns:
        包含 profile_key, hotspot, 及全量配置的字典
    """
    profile_key, hotspot = SPOT_PROFILE_OVERRIDES.get(spot_title, ("default", 1.0))
    profile = dict(PROFILE_LIBRARY[profile_key])
    profile["hotspot"] = hotspot
    profile["profile_key"] = profile_key
    return profile


def _get_hourly_pattern(hour: int, peak_weights: Tuple[float, float, float]) -> float:
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
    morning_w, noon_w, evening_w = peak_weights
    pattern = 0.52
    pattern += morning_w * _circular_hour_gaussian(float(hour), 10.0, 2.2)
    pattern += noon_w * _circular_hour_gaussian(float(hour), 14.5, 3.0)
    pattern += evening_w * _circular_hour_gaussian(float(hour), 20.5, 2.6)
    return pattern


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


def _get_weather_factors() -> Tuple[float, float]:
    """生成随机的天气因素。
    
    Returns:
        (rain_level, temp_factor)
        - rain_level: [0, 1] 表示降雨概率
        - temp_factor: [-1, 1] 表示温度偏差（相对于 18°C）
    """
    # 降雨概率：Beta 分布，倾向于低值
    rain_level = random.betavariate(2.0, 5.0)
    
    # 温度偏差：正态分布
    temp_offset = random.gauss(0, 0.5)  # [-0.5, +0.5] 范围内的单位偏差
    temp_factor = max(-1.0, min(1.0, temp_offset))
    
    return rain_level, temp_factor


def _add_noise(base_value: float, noise_std: float) -> float:
    """添加高斯噪声。
    
    Args:
        base_value: 基础值
        noise_std: 噪声标准差
        
    Returns:
        添加噪声后的值
    """
    noise = random.gauss(0, noise_std)
    return base_value * (1.0 + noise)


def initialize_spot_profiles(spots: List[Dict]) -> None:
    """为景点初始化基础流量缓存。
    
    在启动模拟任务时调用，为每个景点生成一个基础流量值（保持在该景点整个运行期间的稳定性）。
    
    Args:
        spots: 景点列表（每个元素包含 'id', 'title', 'latitude', 'longitude'）
    """
    global _spot_base_flow_cache
    _spot_base_flow_cache.clear()
    
    for spot in spots:
        spot_id = int(spot["id"])
        spot_title = str(spot.get("title", ""))
        profile = _get_spot_profile(spot_title)
        
        # 考虑景点到城市中心的距离（简化：随机地理位置）
        urban_factor = random.uniform(0.72, 1.07)
        
        base_low, base_high = profile["base_range"]
        base_flow = random.uniform(base_low, base_high) * urban_factor * profile["hotspot"]
        _spot_base_flow_cache[spot_id] = max(300.0, min(base_flow * 4.5, 2000.0))


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
    # 获取景点特征
    profile = _get_spot_profile(spot_title)
    base_low, base_high = profile["base_range"]
    
    # 如果缓存中有基础流量，使用缓存；否则实时生成
    if spot_id in _spot_base_flow_cache:
        base_flow = _spot_base_flow_cache[spot_id]
    else:
        urban_factor = random.uniform(0.72, 1.07)
        base_flow = random.uniform(base_low, base_high) * urban_factor * profile["hotspot"]
        base_flow = max(300.0, min(base_flow * 4.5, 2000.0))
    
    # 计算小时内的时间段权重
    hour = current_time.hour
    daily_shape = _get_hourly_pattern(hour, profile["peak_weights"])
    
    # 获取日期修饰符
    is_weekend, is_holiday, is_friday_night = _get_day_modifiers(current_time)
    
    weekend_effect = 1.0 + profile["weekend_boost"] * is_weekend
    holiday_effect = 1.0 + profile["holiday_boost"] * is_holiday
    night_event_effect = 1.0 + profile["night_event_boost"] * (1.0 if is_friday_night else 0.0)
    
    # 获取天气因素
    rain_level, temp_factor = _get_weather_factors()
    weather_effect = max(0.65, min(1.35, 1.0 + profile["rain_coef"] * rain_level + profile["temp_coef"] * temp_factor))
    
    # 综合计算
    flow = base_flow * daily_shape * weekend_effect * holiday_effect * night_event_effect * weather_effect
    
    # 添加噪声
    flow = _add_noise(flow, profile["noise_std"])
    
    # 约束范围
    flow = max(300.0, flow)
    flow = min(max(base_flow * 4.5, 2000.0), flow)
    
    return int(flow)


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
    
    result = {}
    for spot in spots:
        spot_id = int(spot["id"])
        spot_title = str(spot.get("title", ""))
        visitor_count = generate_visitor_count(spot_id, spot_title, current_time)
        result[spot_id] = visitor_count
    
    return result
