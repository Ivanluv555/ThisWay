"""
算法模块

包含核心算法实现：
- crowd_index_calc: 景区拥挤指数计算模型
"""

from .crowd_index_calc import (
    ZoneInput,
    compute_scenic_crowd_index,
    compute_zone_crowd_index,
    DEFAULT_WEATHER_COEFF,
    DEFAULT_MANAGE_COEFF,
    DEFAULT_ZONE_TYPE_KAPPA,
    DEFAULT_SPACE_TYPE_KAPPA,
    DEFAULT_ZONE_TYPE_WEIGHT,
    DEFAULT_SERVICE_ZONE_TYPES,
    DEFAULT_PEAK_ZONE_TYPES,
)

__all__ = [
    "ZoneInput",
    "compute_scenic_crowd_index",
    "compute_zone_crowd_index",
    "DEFAULT_WEATHER_COEFF",
    "DEFAULT_MANAGE_COEFF",
    "DEFAULT_ZONE_TYPE_KAPPA",
    "DEFAULT_SPACE_TYPE_KAPPA",
    "DEFAULT_ZONE_TYPE_WEIGHT",
    "DEFAULT_SERVICE_ZONE_TYPES",
    "DEFAULT_PEAK_ZONE_TYPES",
]
