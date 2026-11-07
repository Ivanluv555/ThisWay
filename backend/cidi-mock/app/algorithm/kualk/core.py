"""
景区游客拥挤指数数学模型 - 核心计算模块

本模块实现了模型的核心计算逻辑，包括：
- 区域负载比计算 (F1, F2)
- 非线性承载压力映射函数 Φ (F3)
- 排队等待修正函数 Ψ (F5)
- 瓶颈效应放大因子 B (F4)
- 拥挤传导项 (F6)
- 区域拥挤指数合成 (F7)
- 景区拥挤指数聚合 (F8-F11)
"""

from dataclasses import dataclass
from typing import Dict, List, Optional, Tuple
import numpy as np
from math import exp, log

try:
    from .models import (
        Zone, ZoneState, ZoneCrowdingResult, ScenicCrowdingResult,
        ScenicArea, Corridor, get_crowding_level, get_perception_weight,
        ZoneType
    )
except ImportError:
    from models import (
        Zone, ZoneState, ZoneCrowdingResult, ScenicCrowdingResult,
        ScenicArea, Corridor, get_crowding_level, get_perception_weight,
        ZoneType
    )


@dataclass
class MappingParams:
    """
    非线性映射函数 Φ(r) 的参数
    
    推荐值基于文档中的经验规律：
    - r_1 = 0.6: 承载量60%阈值
    - r_2 = 0.8: 承载量80%阈值
    - r_3 = 1.2: 极端拥挤饱和点
    """
    r1: float = 0.6      # 舒适→中度拥挤的临界负载比
    r2: float = 0.8      # 中度→重度拥挤的临界负载比
    r3: float = 1.2      # 极端拥挤饱和负载比
    phi1: float = 0.30   # r_1处的拥挤指数
    phi2: float = 0.70   # r_2处的拥挤指数
    p: float = 2.0       # 中段超线性增长指数
    lambda_: float = 5.0 # 高负载段指数增长速率
    
    def __post_init__(self):
        """计算派生系数"""
        self.a1 = self.phi1 / self.r1
        self.a2 = (self.phi2 - self.phi1) / ((self.r2 - self.r1) ** self.p)
        self.a3 = (1.0 - self.phi2) / (exp(self.lambda_ * (self.r3 - self.r2)) - 1)


@dataclass
class QueueParams:
    """
    排队等待修正函数 Ψ(W) 的参数
    """
    w_thresh: float = 30.0  # 等待容忍阈值 (分钟)
    psi_0: float = 0.15     # 阈值内的最大等待贡献
    psi_1: float = 0.20     # 超阈值后的对数增长系数


@dataclass
class BottleneckParams:
    """
    瓶颈效应放大因子 B 的参数
    """
    beta: float = 1.75  # 瓶颈放大指数 (推荐1.5~2.0)


@dataclass
class PropagationParams:
    """
    拥挤传导项参数
    """
    ci_thresh: float = 0.5  # 触发传导的拥挤阈值
    default_delay: float = 15.0  # 默认传导延迟 (分钟)


@dataclass
class AggregationParams:
    """
    景区级聚合参数
    """
    xi: float = 0.6           # 均值-峰值平衡系数
    ci_high_thresh: float = 0.6  # 高负载区域阈值
    delta: float = 0.3        # 耦合放大最大幅度
    q: float = 1.5            # 耦合超线性指数


class ZoneLoadRatioCalculator:
    """
    区域负载比计算器
    
    实现公式:
    - (F1) r_i(t) = N_i(t) / C_i^eff(t)
    - (F2) C_i^eff(t) = C_i^max · κ_i · ∏η_k(t)
    """
    
    @staticmethod
    def compute_effective_capacity(
        zone: Zone,
        weather_factor: float = 1.0,
        event_factor: float = 1.0,
        facility_factor: float = 1.0,
        light_factor: float = 1.0
    ) -> float:
        """
        计算有效承载力
        
        C_i^eff(t) = C_i^max · κ_i · ∏η_k(t)
        
        Args:
            zone: 区域对象
            weather_factor: 天气折减系数
            event_factor: 临时活动占用折减
            facility_factor: 设施运行状态折减
            light_factor: 照明/能见度折减
        
        Returns:
            有效承载力
        """
        reduction = weather_factor * event_factor * facility_factor * light_factor
        return zone.max_capacity * zone.kappa * reduction
    
    @staticmethod
    def compute_load_ratio(
        visitor_count: float,
        effective_capacity: float
    ) -> float:
        """
        计算负载比
        
        r_i(t) = N_i(t) / C_i^eff(t)
        
        Args:
            visitor_count: 实时游客数量
            effective_capacity: 有效承载力
        
        Returns:
            负载比
        """
        if effective_capacity <= 0:
            return float('inf')
        return visitor_count / effective_capacity


class NonlinearMappingFunction:
    """
    非线性承载压力映射函数 Φ
    
    实现公式 (F3):
    
    Φ(r) = {
        a_1 · r                          0 ≤ r < r_1
        Φ(r_1) + a_2 · (r - r_1)^p       r_1 ≤ r < r_2
        Φ(r_2) + a_3 · (e^{λ(r-r_2)} - 1) r_2 ≤ r < r_3
        1.0                              r ≥ r_3
    }
    """
    
    def __init__(self, params: Optional[MappingParams] = None):
        self.params = params or MappingParams()
    
    def __call__(self, r: float) -> float:
        """计算映射值"""
        return self.compute(r)
    
    def compute(self, r: float) -> float:
        """
        计算非线性映射值
        
        Args:
            r: 负载比
        
        Returns:
            拥挤指数基础值 [0, 1]
        """
        p = self.params
        
        if r < 0:
            return 0.0
        elif r < p.r1:
            return p.a1 * r
        elif r < p.r2:
            return p.phi1 + p.a2 * ((r - p.r1) ** p.p)
        elif r < p.r3:
            return p.phi2 + p.a3 * (exp(p.lambda_ * (r - p.r2)) - 1)
        else:
            return 1.0
    
    def compute_derivative(self, r: float) -> float:
        """
        计算映射函数的导数（用于分析）
        
        Args:
            r: 负载比
        
        Returns:
            导数值
        """
        p = self.params
        
        if r < 0:
            return 0.0
        elif r < p.r1:
            return p.a1
        elif r < p.r2:
            return p.a2 * p.p * ((r - p.r1) ** (p.p - 1))
        elif r < p.r3:
            return p.a3 * p.lambda_ * exp(p.lambda_ * (r - p.r2))
        else:
            return 0.0


class QueueWaitingCorrection:
    """
    排队等待修正函数 Ψ
    
    实现公式 (F5):
    
    Ψ(W_i) = {
        (W_i / W_thresh) · ψ_0           W_i ≤ W_thresh
        ψ_0 + ψ_1 · ln(1 + (W_i - W_thresh) / W_thresh)  W_i > W_thresh
    }
    """
    
    def __init__(self, params: Optional[QueueParams] = None):
        self.params = params or QueueParams()
    
    def __call__(self, waiting_time: float) -> float:
        """计算排队等待贡献"""
        return self.compute(waiting_time)
    
    def compute(self, waiting_time: float) -> float:
        """
        计算排队等待感知增量
        
        Args:
            waiting_time: 等待时间 (分钟)
        
        Returns:
            排队等待贡献
        """
        p = self.params
        
        if waiting_time <= 0:
            return 0.0
        elif waiting_time <= p.w_thresh:
            return (waiting_time / p.w_thresh) * p.psi_0
        else:
            excess_ratio = (waiting_time - p.w_thresh) / p.w_thresh
            return p.psi_0 + p.psi_1 * log(1 + excess_ratio)
    
    @staticmethod
    def compute_waiting_time(
        queue_count: float,
        service_rate: float,
        service_windows: int
    ) -> float:
        """
        计算排队等待时间
        
        W_i(t) = N_i^queue(t) / (μ_i · s_i)
        
        Args:
            queue_count: 排队人数
            service_rate: 单窗口服务速率 (人/分钟)
            service_windows: 服务窗口数
        
        Returns:
            等待时间 (分钟)
        """
        if service_rate <= 0 or service_windows <= 0:
            return float('inf')
        return queue_count / (service_rate * service_windows)


class BottleneckAmplificationFactor:
    """
    瓶颈效应放大因子 B
    
    实现公式 (F4):
    
    B_i(t) = {
        1                                D_i^out ≤ Q_i^out,max
        (D_i^out / Q_i^out,max)^β       D_i^out > Q_i^out,max
    }
    """
    
    def __init__(self, params: Optional[BottleneckParams] = None):
        self.params = params or BottleneckParams()
    
    def __call__(self, outflow_demand: float, outflow_capacity: float) -> float:
        """计算瓶颈放大因子"""
        return self.compute(outflow_demand, outflow_capacity)
    
    def compute(self, outflow_demand: float, outflow_capacity: float) -> float:
        """
        计算瓶颈放大因子
        
        Args:
            outflow_demand: 出流需求 (人/分钟)
            outflow_capacity: 出流能力 (人/分钟)
        
        Returns:
            瓶颈放大因子
        """
        if outflow_capacity <= 0:
            return float('inf')
        
        if outflow_demand <= outflow_capacity:
            return 1.0
        
        ratio = outflow_demand / outflow_capacity
        return ratio ** self.params.beta


class CrowdingPropagation:
    """
    拥挤传导项
    
    实现公式 (F6):
    
    ΔCI_i^prop(t) = Σ_j α_ji · max(0, CI_j(t - Δt_ji) - CI_thresh)
    """
    
    def __init__(self, params: Optional[PropagationParams] = None):
        self.params = params or PropagationParams()
    
    def compute(
        self,
        upstream_crowding_indices: Dict[str, float],
        propagation_coefficients: Dict[str, float],
        delays: Optional[Dict[str, float]] = None
    ) -> float:
        """
        计算拥挤传导增量
        
        Args:
            upstream_crowding_indices: 上游区域拥挤指数字典 {zone_id: CI}
            propagation_coefficients: 传导衰减系数 {zone_id: α}
            delays: 传导延迟 (分钟)，暂不使用，预留接口
        
        Returns:
            拥挤传导增量
        """
        total = 0.0
        
        for zone_id, ci in upstream_crowding_indices.items():
            if ci > self.params.ci_thresh:
                alpha = propagation_coefficients.get(zone_id, 0.1)
                total += alpha * (ci - self.params.ci_thresh)
        
        return total


class ZoneCrowdingIndexCalculator:
    """
    区域拥挤指数计算器
    
    实现公式 (F7):
    
    CI_i(t) = min(1.0, Φ(r_i) · B_i + Ψ(W_i) + ΔCI_i^prop)
    """
    
    def __init__(
        self,
        mapping_params: Optional[MappingParams] = None,
        queue_params: Optional[QueueParams] = None,
        bottleneck_params: Optional[BottleneckParams] = None,
        propagation_params: Optional[PropagationParams] = None
    ):
        self.mapping_func = NonlinearMappingFunction(mapping_params)
        self.queue_func = QueueWaitingCorrection(queue_params)
        self.bottleneck_func = BottleneckAmplificationFactor(bottleneck_params)
        self.propagation = CrowdingPropagation(propagation_params)
    
    def compute(
        self,
        zone: Zone,
        state: ZoneState,
        outflow_demand: float = 0.0,
        outflow_capacity: float = float('inf'),
        upstream_crowding_indices: Optional[Dict[str, float]] = None,
        propagation_coefficients: Optional[Dict[str, float]] = None
    ) -> ZoneCrowdingResult:
        """
        计算区域拥挤指数
        
        Args:
            zone: 区域对象
            state: 区域状态
            outflow_demand: 出流需求 (人/分钟)
            outflow_capacity: 出流能力 (人/分钟)
            upstream_crowding_indices: 上游区域拥挤指数
            propagation_coefficients: 传导衰减系数
        
        Returns:
            区域拥挤指数计算结果
        """
        effective_capacity = ZoneLoadRatioCalculator.compute_effective_capacity(
            zone,
            weather_factor=state.weather_factor,
            event_factor=state.event_factor,
            facility_factor=state.facility_factor,
            light_factor=state.light_factor
        )
        
        load_ratio = ZoneLoadRatioCalculator.compute_load_ratio(
            state.visitor_count,
            effective_capacity
        )
        
        base_crowding = self.mapping_func(load_ratio)
        
        bottleneck_factor = self.bottleneck_func(outflow_demand, outflow_capacity)
        
        queue_contribution = 0.0
        if zone.is_service_zone and zone.service_rate > 0:
            waiting_time = QueueWaitingCorrection.compute_waiting_time(
                state.queue_count,
                zone.service_rate,
                zone.service_windows
            )
            queue_contribution = self.queue_func(waiting_time)
        
        propagation_contribution = 0.0
        if upstream_crowding_indices:
            propagation_contribution = self.propagation.compute(
                upstream_crowding_indices,
                propagation_coefficients or {}
            )
        
        crowding_index = min(1.0, base_crowding * bottleneck_factor + queue_contribution + propagation_contribution)
        
        return ZoneCrowdingResult(
            zone_id=zone.zone_id,
            timestamp=state.timestamp,
            visitor_count=state.visitor_count,
            effective_capacity=effective_capacity,
            load_ratio=load_ratio,
            base_crowding=base_crowding,
            bottleneck_factor=bottleneck_factor,
            queue_contribution=queue_contribution,
            propagation_contribution=propagation_contribution,
            crowding_index=crowding_index,
            crowding_level=get_crowding_level(crowding_index)
        )


class ScenicCrowdingAggregator:
    """
    景区拥挤指数聚合器
    
    实现公式 (F8-F11):
    - (F8) SCI_s^base = Σw_i·CI_i / Σw_i
    - (F9) SCI_s^raw = ξ·SCI_s^base + (1-ξ)·SCI_s^peak
    - (F10) Γ_s = 1 + δ·(n_high/N_s)^q
    - (F11) SCI_s = min(1, SCI_s^raw · Γ_s)
    """
    
    def __init__(self, params: Optional[AggregationParams] = None):
        self.params = params or AggregationParams()
    
    def aggregate(
        self,
        zone_results: Dict[str, ZoneCrowdingResult],
        zones: Dict[str, Zone]
    ) -> ScenicCrowdingResult:
        """
        聚合计算景区拥挤指数
        
        Args:
            zone_results: 各区域拥挤指数计算结果
            zones: 区域字典
        
        Returns:
            景区拥挤指数计算结果
        """
        if not zone_results:
            return ScenicCrowdingResult(scenic_id="", timestamp=0.0)
        
        timestamp = next(iter(zone_results.values())).timestamp
        
        weighted_sum = 0.0
        weight_total = 0.0
        peak_index = 0.0
        
        peak_zone_types = {ZoneType.CORE, ZoneType.ENTRY, ZoneType.RIDE}
        
        for zone_id, result in zone_results.items():
            zone = zones.get(zone_id)
            weight = get_perception_weight(zone.zone_type) if zone else 1.0
            weighted_sum += weight * result.crowding_index
            weight_total += weight
            
            if zone and zone.zone_type in peak_zone_types:
                peak_index = max(peak_index, result.crowding_index)
        
        if weight_total > 0:
            base_index = weighted_sum / weight_total
        else:
            base_index = 0.0
        
        raw_index = self.params.xi * base_index + (1 - self.params.xi) * peak_index
        
        n_high = sum(1 for r in zone_results.values() if r.crowding_index > self.params.ci_high_thresh)
        n_total = len(zone_results)
        
        if n_total > 0:
            high_ratio = n_high / n_total
            coupling_factor = 1 + self.params.delta * (high_ratio ** self.params.q)
        else:
            coupling_factor = 1.0
        
        crowding_index = min(1.0, raw_index * coupling_factor)
        
        result = ScenicCrowdingResult(
            scenic_id="",
            timestamp=timestamp,
            zone_results=zone_results,
            base_index=base_index,
            peak_index=peak_index,
            raw_index=raw_index,
            coupling_factor=coupling_factor,
            crowding_index=crowding_index,
            crowding_level=get_crowding_level(crowding_index)
        )
        
        return result


class CrowdingIndexModel:
    """
    拥挤指数模型主类
    
    整合所有计算模块，提供统一的计算接口
    """
    
    def __init__(
        self,
        mapping_params: Optional[MappingParams] = None,
        queue_params: Optional[QueueParams] = None,
        bottleneck_params: Optional[BottleneckParams] = None,
        propagation_params: Optional[PropagationParams] = None,
        aggregation_params: Optional[AggregationParams] = None
    ):
        self.zone_calculator = ZoneCrowdingIndexCalculator(
            mapping_params=mapping_params,
            queue_params=queue_params,
            bottleneck_params=bottleneck_params,
            propagation_params=propagation_params
        )
        self.aggregator = ScenicCrowdingAggregator(aggregation_params)
    
    def compute_zone_crowding(
        self,
        zone: Zone,
        state: ZoneState,
        outflow_demand: float = 0.0,
        outflow_capacity: float = float('inf'),
        upstream_crowding_indices: Optional[Dict[str, float]] = None,
        propagation_coefficients: Optional[Dict[str, float]] = None
    ) -> ZoneCrowdingResult:
        """
        计算单个区域的拥挤指数
        """
        return self.zone_calculator.compute(
            zone=zone,
            state=state,
            outflow_demand=outflow_demand,
            outflow_capacity=outflow_capacity,
            upstream_crowding_indices=upstream_crowding_indices,
            propagation_coefficients=propagation_coefficients
        )
    
    def compute_scenic_crowding(
        self,
        scenic_area: ScenicArea,
        zone_states: Dict[str, ZoneState],
        outflow_demands: Optional[Dict[str, float]] = None,
        outflow_capacities: Optional[Dict[str, float]] = None,
        upstream_crowding_history: Optional[Dict[str, Dict[str, float]]] = None
    ) -> ScenicCrowdingResult:
        """
        计算景区整体拥挤指数
        
        Args:
            scenic_area: 景区对象
            zone_states: 各区域状态字典
            outflow_demands: 各区域出流需求
            outflow_capacities: 各区域出流能力
            upstream_crowding_history: 上游区域历史拥挤指数
        
        Returns:
            景区拥挤指数计算结果
        """
        zone_results = {}
        
        for zone_id, state in zone_states.items():
            zone = scenic_area.zones.get(zone_id)
            if zone is None:
                continue
            
            outflow_demand = (outflow_demands or {}).get(zone_id, 0.0)
            outflow_capacity = (outflow_capacities or {}).get(zone_id, float('inf'))
            
            upstream_crowding = None
            if upstream_crowding_history:
                upstream_zones = scenic_area.get_upstream_zones(zone_id)
                upstream_crowding = {
                    uid: upstream_crowding_history.get(uid, {}).get('crowding_index', 0.0)
                    for uid in upstream_zones if uid in upstream_crowding_history
                }
            
            result = self.compute_zone_crowding(
                zone=zone,
                state=state,
                outflow_demand=outflow_demand,
                outflow_capacity=outflow_capacity,
                upstream_crowding_indices=upstream_crowding
            )
            zone_results[zone_id] = result
        
        scenic_result = self.aggregator.aggregate(zone_results, scenic_area.zones)
        scenic_result.scenic_id = scenic_area.scenic_id
        
        return scenic_result
