"""
景区游客拥挤指数数学模型 - 多景区联动模块

本模块实现多景区联动模型，包括：
- 跨景区溢出效应模型
- 转移概率模型 (Logit模型)
- 多景区状态矩阵
"""

from dataclasses import dataclass, field
from typing import Dict, List, Optional, Tuple
from math import exp

try:
    from .models import ScenicArea, MultiScenicState
    from .core import ScenicCrowdingResult
except ImportError:
    from models import ScenicArea, MultiScenicState
    from core import ScenicCrowdingResult


@dataclass
class ScenicAttributes:
    """
    景区属性
    """
    scenic_id: str
    name: str
    attractiveness: float       # 景区吸引力 (0-100)
    total_capacity: float       # 总承载量
    location: Tuple[float, float] = (0.0, 0.0)  # 经纬度坐标


@dataclass
class SpilloverParams:
    """
    溢出效应参数
    """
    theta: float = 0.3          # 溢出比例系数
    delay_hours: float = 2.0    # 跨景区传导延迟 (小时)


@dataclass
class TransferUtilityWeights:
    """
    转移效用函数权重
    """
    omega_1: float = 1.0        # 吸引力权重
    omega_2: float = 0.5        # 距离权重 (负效用)
    omega_3: float = 0.8        # 拥挤度权重 (负效用)
    omega_4: float = 0.3        # 剩余容量权重


def compute_distance(
    loc_a: Tuple[float, float],
    loc_b: Tuple[float, float]
) -> float:
    """
    计算两个景区之间的距离 (简化的欧氏距离)
    
    实际应用中应使用Haversine公式计算球面距离
    
    Args:
        loc_a: 景区A坐标 (经度, 纬度)
        loc_b: 景区B坐标 (经度, 纬度)
    
    Returns:
        距离 (km)
    """
    import math
    lat1, lon1 = loc_a[1], loc_a[0]
    lat2, lon2 = loc_b[1], loc_b[0]
    
    lat1_rad = math.radians(lat1)
    lat2_rad = math.radians(lat2)
    delta_lat = math.radians(lat2 - lat1)
    delta_lon = math.radians(lon2 - lon1)
    
    a = math.sin(delta_lat / 2) ** 2 + math.cos(lat1_rad) * math.cos(lat2_rad) * math.sin(delta_lon / 2) ** 2
    c = 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))
    
    R = 6371  # 地球半径 (km)
    return R * c


class TransferProbabilityModel:
    """
    转移概率模型 (Logit模型)
    
    P_{A→B} = exp(U_B) / Σ_k exp(U_k)
    
    效用函数:
    U_k = ω_1·Attract_k - ω_2·Dist_{Ak} - ω_3·SCI_k(t) + ω_4·Capacity_Remain_k
    """
    
    def __init__(self, weights: Optional[TransferUtilityWeights] = None):
        self.weights = weights or TransferUtilityWeights()
    
    def compute_utility(
        self,
        target_attrs: ScenicAttributes,
        source_location: Tuple[float, float],
        target_crowding_index: float,
        target_remaining_capacity: float
    ) -> float:
        """
        计算目标景区的效用
        
        Args:
            target_attrs: 目标景区属性
            source_location: 源景区位置
            target_crowding_index: 目标景区拥挤指数
            target_remaining_capacity: 目标景区剩余容量
        
        Returns:
            效用值
        """
        w = self.weights
        
        distance = compute_distance(source_location, target_attrs.location)
        
        utility = (
            w.omega_1 * target_attrs.attractiveness
            - w.omega_2 * distance
            - w.omega_3 * target_crowding_index * 100  # 归一化
            + w.omega_4 * (target_remaining_capacity / target_attrs.total_capacity) * 100
        )
        
        return utility
    
    def compute_probabilities(
        self,
        source_attrs: ScenicAttributes,
        target_attrs_list: List[ScenicAttributes],
        target_crowding_indices: Dict[str, float],
        target_remaining_capacities: Dict[str, float]
    ) -> Dict[str, float]:
        """
        计算从源景区到各目标景区的转移概率
        
        Args:
            source_attrs: 源景区属性
            target_attrs_list: 目标景区属性列表
            target_crowding_indices: 各目标景区拥挤指数
            target_remaining_capacities: 各目标景区剩余容量
        
        Returns:
            转移概率字典 {target_id: probability}
        """
        utilities = {}
        
        for target in target_attrs_list:
            utility = self.compute_utility(
                target_attrs=target,
                source_location=source_attrs.location,
                target_crowding_index=target_crowding_indices.get(target.scenic_id, 0.0),
                target_remaining_capacity=target_remaining_capacities.get(target.scenic_id, 0.0)
            )
            utilities[target.scenic_id] = utility
        
        exp_utilities = {tid: exp(u) for tid, u in utilities.items()}
        total_exp = sum(exp_utilities.values())
        
        if total_exp == 0:
            n = len(target_attrs_list)
            return {t.scenic_id: 1.0 / n for t in target_attrs_list}
        
        probabilities = {tid: eu / total_exp for tid, eu in exp_utilities.items()}
        
        return probabilities


class SpilloverModel:
    """
    跨景区溢出效应模型
    
    ΔN_B^{spill}(t) = θ_{AB} · max(0, N_A^{total}(t - Δt_{AB}) - C_A^{total}) · P_{A→B}
    """
    
    def __init__(
        self,
        params: Optional[SpilloverParams] = None,
        utility_weights: Optional[TransferUtilityWeights] = None
    ):
        self.params = params or SpilloverParams()
        self.transfer_model = TransferProbabilityModel(utility_weights)
    
    def compute_spillover(
        self,
        source_attrs: ScenicAttributes,
        source_visitor_count: float,
        target_attrs_list: List[ScenicAttributes],
        target_crowding_indices: Dict[str, float],
        target_remaining_capacities: Dict[str, float]
    ) -> Dict[str, float]:
        """
        计算溢出客流
        
        Args:
            source_attrs: 源景区属性
            source_visitor_count: 源景区当前游客数
            target_attrs_list: 目标景区属性列表
            target_crowding_indices: 各目标景区拥挤指数
            target_remaining_capacities: 各目标景区剩余容量
        
        Returns:
            各目标景区的溢出客流 {target_id: spillover_count}
        """
        overflow = max(0, source_visitor_count - source_attrs.total_capacity)
        
        if overflow <= 0:
            return {t.scenic_id: 0.0 for t in target_attrs_list}
        
        probabilities = self.transfer_model.compute_probabilities(
            source_attrs=source_attrs,
            target_attrs_list=target_attrs_list,
            target_crowding_indices=target_crowding_indices,
            target_remaining_capacities=target_remaining_capacities
        )
        
        spillover = {}
        for target in target_attrs_list:
            spillover[target.scenic_id] = (
                self.params.theta * overflow * probabilities.get(target.scenic_id, 0.0)
            )
        
        return spillover


class MultiScenicCoordinator:
    """
    多景区联动协调器
    
    管理多个景区的状态，计算溢出效应，生成状态矩阵
    """
    
    def __init__(
        self,
        spillover_params: Optional[SpilloverParams] = None,
        utility_weights: Optional[TransferUtilityWeights] = None
    ):
        self.spillover_model = SpilloverModel(spillover_params, utility_weights)
        self.scenic_attrs: Dict[str, ScenicAttributes] = {}
        self.scenic_results: Dict[str, ScenicCrowdingResult] = {}
        self.visitor_counts: Dict[str, float] = {}
    
    def add_scenic_area(
        self,
        scenic_attrs: ScenicAttributes,
        scenic_result: Optional[ScenicCrowdingResult] = None,
        visitor_count: float = 0.0
    ) -> None:
        """
        添加景区
        
        Args:
            scenic_attrs: 景区属性
            scenic_result: 景区拥挤指数计算结果
            visitor_count: 当前游客数
        """
        self.scenic_attrs[scenic_attrs.scenic_id] = scenic_attrs
        if scenic_result:
            self.scenic_results[scenic_attrs.scenic_id] = scenic_result
        self.visitor_counts[scenic_attrs.scenic_id] = visitor_count
    
    def update_scenic_state(
        self,
        scenic_id: str,
        scenic_result: ScenicCrowdingResult,
        visitor_count: float
    ) -> None:
        """
        更新景区状态
        
        Args:
            scenic_id: 景区ID
            scenic_result: 景区拥挤指数计算结果
            visitor_count: 当前游客数
        """
        self.scenic_results[scenic_id] = scenic_result
        self.visitor_counts[scenic_id] = visitor_count
    
    def compute_all_spillovers(self) -> Dict[Tuple[str, str], float]:
        """
        计算所有景区间的溢出效应
        
        Returns:
            溢出客流字典 {(source_id, target_id): spillover_count}
        """
        all_spillovers = {}
        
        for source_id, source_attrs in self.scenic_attrs.items():
            source_visitor_count = self.visitor_counts.get(source_id, 0.0)
            
            target_attrs_list = [
                attrs for sid, attrs in self.scenic_attrs.items()
                if sid != source_id
            ]
            
            if not target_attrs_list:
                continue
            
            target_crowding_indices = {
                sid: result.crowding_index
                for sid, result in self.scenic_results.items()
                if sid != source_id
            }
            
            target_remaining_capacities = {
                sid: attrs.total_capacity - self.visitor_counts.get(sid, 0.0)
                for sid, attrs in self.scenic_attrs.items()
                if sid != source_id
            }
            
            spillovers = self.spillover_model.compute_spillover(
                source_attrs=source_attrs,
                source_visitor_count=source_visitor_count,
                target_attrs_list=target_attrs_list,
                target_crowding_indices=target_crowding_indices,
                target_remaining_capacities=target_remaining_capacities
            )
            
            for target_id, spillover_count in spillovers.items():
                all_spillovers[(source_id, target_id)] = spillover_count
        
        return all_spillovers
    
    def get_state_matrix(self, timestamp: float) -> MultiScenicState:
        """
        获取多景区状态矩阵
        
        Args:
            timestamp: 时间戳
        
        Returns:
            多景区状态
        """
        scenic_indices = {
            sid: result.crowding_index
            for sid, result in self.scenic_results.items()
        }
        
        remaining_capacities = {
            sid: attrs.total_capacity - self.visitor_counts.get(sid, 0.0)
            for sid, attrs in self.scenic_attrs.items()
        }
        
        spill_over_flows = self.compute_all_spillovers()
        
        return MultiScenicState(
            timestamp=timestamp,
            scenic_indices=scenic_indices,
            remaining_capacities=remaining_capacities,
            spill_over_flows=spill_over_flows
        )
    
    def get_recommendations(self) -> Dict[str, List[str]]:
        """
        获取分流推荐
        
        基于当前状态，为每个拥挤景区推荐分流目标
        
        Returns:
            分流推荐字典 {source_id: [recommended_target_ids]}
        """
        recommendations = {}
        
        for source_id, result in self.scenic_results.items():
            if result.crowding_index < 0.7:
                continue
            
            source_attrs = self.scenic_attrs[source_id]
            
            target_attrs_list = [
                attrs for sid, attrs in self.scenic_attrs.items()
                if sid != source_id
            ]
            
            target_crowding_indices = {
                sid: r.crowding_index
                for sid, r in self.scenic_results.items()
                if sid != source_id
            }
            
            target_remaining_capacities = {
                sid: attrs.total_capacity - self.visitor_counts.get(sid, 0.0)
                for sid, attrs in self.scenic_attrs.items()
                if sid != source_id
            }
            
            probabilities = self.spillover_model.transfer_model.compute_probabilities(
                source_attrs=source_attrs,
                target_attrs_list=target_attrs_list,
                target_crowding_indices=target_crowding_indices,
                target_remaining_capacities=target_remaining_capacities
            )
            
            sorted_targets = sorted(
                probabilities.items(),
                key=lambda x: x[1],
                reverse=True
            )
            
            recommendations[source_id] = [tid for tid, _ in sorted_targets[:3]]
        
        return recommendations
