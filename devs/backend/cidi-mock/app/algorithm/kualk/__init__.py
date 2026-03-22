"""
拥挤指数计算模块

本模块包含基于数学建模的完整拥挤指数计算模型
"""

from enum import Enum
from dataclasses import dataclass
from typing import Optional, List, Union

from .models import (
    Zone, ZoneType, SpaceForm, ZoneState,
    ScenicArea, Corridor,
    ZoneCrowdingResult, ScenicCrowdingResult,
    CrowdingLevel, WeatherType,
    get_crowding_level, get_perception_weight, get_weather_factor
)

from .core import (
    MappingParams, NonlinearMappingFunction,
    QueueParams, QueueWaitingCorrection,
    BottleneckParams, BottleneckAmplificationFactor,
    PropagationParams, CrowdingPropagation,
    ZoneLoadRatioCalculator, ZoneCrowdingIndexCalculator,
    ScenicCrowdingAggregator, CrowdingIndexModel
)

from .multi_scenic import (
    ScenicAttributes, SpilloverParams, TransferUtilityWeights,
    TransferProbabilityModel, SpilloverModel, MultiScenicCoordinator
)

__version__ = "2.0.0"
__author__ = "UIEP Team"

__all__ = [
    # 数据结构
    "Zone",
    "ZoneType",
    "SpaceForm",
    "ZoneState",
    "ScenicArea",
    "Corridor",
    "ZoneCrowdingResult",
    "ScenicCrowdingResult",
    "CrowdingLevel",
    "WeatherType",
    
    # 工具函数
    "get_crowding_level",
    "get_perception_weight",
    "get_weather_factor",
    
    # 参数类
    "MappingParams",
    "QueueParams",
    "BottleneckParams",
    "PropagationParams",
    "SpilloverParams",
    "TransferUtilityWeights",
    
    # 核心计算类
    "NonlinearMappingFunction",
    "QueueWaitingCorrection",
    "BottleneckAmplificationFactor",
    "CrowdingPropagation",
    "ZoneLoadRatioCalculator",
    "ZoneCrowdingIndexCalculator",
    "ScenicCrowdingAggregator",
    "CrowdingIndexModel",
    
    # 多景区模型
    "ScenicAttributes",
    "TransferProbabilityModel",
    "SpilloverModel",
    "MultiScenicCoordinator"
]
