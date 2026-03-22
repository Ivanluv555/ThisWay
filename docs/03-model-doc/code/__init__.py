"""
景区游客拥挤指数数学模型

本包实现了景区游客拥挤指数的完整数学模型体系，包括：
- 区域级拥挤指数计算
- 景区级拥挤指数聚合
- 多景区联动模型

使用示例:
    from code import CrowdingIndexModel, ScenicArea, Zone, ZoneState
    
    model = CrowdingIndexModel()
    
    zone = Zone(
        zone_id="core_1",
        name="核心景点",
        zone_type=ZoneType.CORE,
        area=1000.0
    )
    
    state = ZoneState(
        zone_id="core_1",
        timestamp=0.0,
        visitor_count=500.0
    )
    
    result = model.compute_zone_crowding(zone, state)
    print(f"拥挤指数: {result.crowding_index:.2f}")
"""

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

__version__ = "1.0.0"
__author__ = "UIEP Team"

__all__ = [
    # 数据结构
    "Zone", "ZoneType", "SpaceForm", "ZoneState",
    "ScenicArea", "Corridor",
    "ZoneCrowdingResult", "ScenicCrowdingResult",
    "CrowdingLevel", "WeatherType",
    
    # 工具函数
    "get_crowding_level", "get_perception_weight", "get_weather_factor",
    
    # 参数类
    "MappingParams", "QueueParams", "BottleneckParams",
    "PropagationParams", "SpilloverParams", "TransferUtilityWeights",
    
    # 核心计算类
    "NonlinearMappingFunction", "QueueWaitingCorrection",
    "BottleneckAmplificationFactor", "CrowdingPropagation",
    "ZoneLoadRatioCalculator", "ZoneCrowdingIndexCalculator",
    "ScenicCrowdingAggregator", "CrowdingIndexModel",
    
    # 多景区模型
    "ScenicAttributes", "TransferProbabilityModel",
    "SpilloverModel", "MultiScenicCoordinator"
]
