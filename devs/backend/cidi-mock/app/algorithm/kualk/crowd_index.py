from enum import Enum
from dataclasses import dataclass
from typing import Optional, List, Union


class SpotType(Enum):
    """景区类型 (用于计算综合负荷指数权重)"""
    NATURAL = "natural"         # 自然景区（空间优先）
    CULTURAL = "cultural"       # 人文景区（服务 + 设施优先）
    COMPREHENSIVE = "comprehensive" # 综合景区（均衡型）


class InfraSpotType(Enum):
    """基础设施景区类型 (用于计算基础设施负荷指数权重)"""
    LEISURE = "leisure"         # 休闲型景区（侧重便利）
    EXPERIENCE = "experience"   # 体验型景区（侧重项目）
    COMPREHENSIVE = "comprehensive" # 综合型景区


class ActivityType(Enum):
    """活动类型 (用于计算人均合理活动面积)"""
    DYNAMIC = "dynamic"         # 动态活动类（如游乐设施、互动体验项目）
    STATIC = "static"           # 静态观赏类（如观景台、花卉园、展馆）
    LINEAR = "linear"           # 线性步道类（如登山步道、玻璃栈道）


@dataclass
class CrowdIndexResult:
    ci: float               # 综合承载压力指数
    v_index: float          # 游客承载压力指数
    s_index: float          # 服务人员承载压力指数
    i_index: float          # 基础设施承载压力指数
    level: str              # 拥挤等级 (宽松, 适中, 轻度超载, 严重超载)


class CrowdIndexCalculator:
    """
    景区拥挤指数计算器
    基于数学建模：CI = Wv * V + Ws * S + Wi * I
    """

    def __init__(self):
        pass

    @staticmethod
    def get_ci_weights(spot_type: SpotType):
        """
        获取综合负荷指数权重 (Wv, Ws, Wi)
        :param spot_type: 景区类型
        :return: (Wv, Ws, Wi)
        """
        if spot_type == SpotType.NATURAL:
            return 0.5, 0.2, 0.3
        elif spot_type == SpotType.CULTURAL:
            return 0.25, 0.4, 0.35
        elif spot_type == SpotType.COMPREHENSIVE:
            return 0.4, 0.3, 0.3
        return 0.4, 0.3, 0.3  # Default

    @staticmethod
    def get_infra_weights(infra_type: InfraSpotType):
        """
        获取基础设施权重 (Wip, Wit)
        :param infra_type: 基础设施景区类型
        :return: (Wip, Wit)
        """
        if infra_type == InfraSpotType.LEISURE:
            return 0.6, 0.4
        elif infra_type == InfraSpotType.EXPERIENCE:
            return 0.3, 0.7
        elif infra_type == InfraSpotType.COMPREHENSIVE:
            return 0.5, 0.5
        return 0.5, 0.5  # Default

    @staticmethod
    def get_avg_area_per_person(activity_type: ActivityType) -> float:
        """
        获取人均合理活动面积 (S_bar)
        :param activity_type: 活动类型
        :return: m^2/人 (取中间值)
        """
        if activity_type == ActivityType.DYNAMIC:
            return 4.0  # 3~5
        elif activity_type == ActivityType.STATIC:
            return 6.5  # 5~8
        elif activity_type == ActivityType.LINEAR:
            return 2.5  # 2~3
        return 4.0

    @staticmethod
    def get_crowd_level(ci: float) -> str:
        """
        根据CI指数获取拥挤等级
        """
        if ci <= 2:
            return "宽松"
        elif ci <= 5:
            return "适中"
        elif ci <= 8:
            return "轻度超载"
        else:
            return "严重超载"

    @classmethod
    def calculate_visitor_index(cls, N: int, S_e: float, activity_type: ActivityType,
                              alpha: float = 1.0, beta: float = 1.0) -> float:
        """
        计算游客承载压力指数 (V)
        V = min(10, 10 * (N/C) * alpha * beta)
        C = S_e / S_bar

        :param N: 实时游客人数
        :param S_e: 项目有效面积 (m^2)
        :param activity_type: 活动类型
        :param alpha: 空间分布参数 (1.0~1.5)
        :param beta: 流动性参数 (0.8~1.2)
        :return: V (0~10)
        """
        s_bar = cls.get_avg_area_per_person(activity_type)
        if s_bar <= 0: return 10.0 # Prevent division by zero
        
        C = S_e / s_bar
        if C <= 0: return 10.0 # Should not happen if S_e > 0
        
        V = 10 * (N / C) * alpha * beta
        return min(10.0, V)

    @classmethod
    def calculate_staff_index(cls, N: int, M: int, S_y: float = 35.0, S_n: float = 50.0) -> float:
        """
        计算服务人员承载压力指数 (S)
        
        :param N: 实时游客人数
        :param M: 工作人员数
        :param S_y: 每名工作人员合理服务配比 (人/人)
        :param S_n: 每名工作人员警戒服务配比 (人/人)
        :return: S (0~10)
        """
        if M <= 0: return 10.0 # No staff means max pressure if there are visitors
        if N <= 0: return 0.0
        
        # 避免除零错误 (S_n - S_y should be > 0)
        if S_n <= S_y: S_n = S_y + 1
        
        val_Sy = M * S_y
        val_Sn = M * S_n
        
        if N <= val_Sy:
            return 0.0
        elif N <= val_Sn:
            return 10 * ((N / M) - S_y) / (S_n - S_y)
        else:
            return 10.0

    @classmethod
    def calculate_infrastructure_index(cls, N: int, 
                                     F_p: int, F_t: int, 
                                     infra_type: InfraSpotType) -> float:
        """
        计算基础设施承载压力指数 (I)
        I = Wip * Ip + Wit * It
        
        :param N: 实时游客人数
        :param F_p: 公共设施总服务能力
        :param F_t: 旅游设施总服务能力
        :param infra_type: 基础设施景区类型
        :return: I (0~10)
        """
        W_ip, W_it = cls.get_infra_weights(infra_type)
        
        # 计算 Ip (公共设施负载指数)
        # N <= Fp * 0.7 -> 0
        # Fp * 0.7 < N <= Fp * 1.4 -> linear 0~10
        # N > Fp * 1.4 -> 10
        I_p = 0.0
        if F_p > 0:
            limit_low_p = F_p * 0.7
            limit_high_p = F_p * 1.4
            
            if N <= limit_low_p:
                I_p = 0.0
            elif N <= limit_high_p:
                I_p = ((N / F_p) - 0.7) / (1.4 - 0.7) * 10
            else:
                I_p = 10.0
        else:
            I_p = 10.0 if N > 0 else 0.0
            
        # 计算 It (旅游设施负载指数)
        # N <= Ft * 0.6 -> 0
        # Ft * 0.6 < N <= Ft * 1.2 -> linear 0~10
        # N > Ft * 1.2 -> 10
        # Note: Corrected lower bound to 0.6 based on continuity with range [0.6, 1.2]
        I_t = 0.0
        if F_t > 0:
            limit_low_t = F_t * 0.6
            limit_high_t = F_t * 1.2
            
            if N <= limit_low_t:
                I_t = 0.0
            elif N <= limit_high_t:
                I_t = ((N / F_t) - 0.6) / (1.2 - 0.6) * 10
            else:
                I_t = 10.0
        else:
            I_t = 10.0 if N > 0 else 0.0
            
        return W_ip * I_p + W_it * I_t

    @classmethod
    def calculate(cls, 
                  spot_type: SpotType,
                  infra_type: InfraSpotType,
                  activity_type: ActivityType,
                  N: int,
                  S_e: float,
                  M: int,
                  F_p: int,
                  F_t: int,
                  alpha: float = 1.0,
                  beta: float = 1.0,
                  S_y: float = 35.0,
                  S_n: float = 50.0) -> CrowdIndexResult:
        """
        计算综合拥挤指数 (Main Entry)
        
        :param spot_type: 景区类型 (自然/人文/综合)
        :param infra_type: 基础设施类型 (休闲/体验/综合)
        :param activity_type: 活动类型 (动态/静态/线性)
        :param N: 实时游客人数
        :param S_e: 项目有效面积 (m^2)
        :param M: 工作人员数
        :param F_p: 公共设施总服务能力
        :param F_t: 旅游设施总服务能力
        :param alpha: 空间分布参数 (1.0~1.5)
        :param beta: 流动性参数 (0.8~1.2)
        :param S_y: 工作人员合理服务配比
        :param S_n: 工作人员警戒服务配比
        :return: CrowdIndexResult
        """
        
        # 1. Calculate Weights
        W_v, W_s, W_i = cls.get_ci_weights(spot_type)
        
        # 2. Calculate Individual Indices
        V = cls.calculate_visitor_index(N, S_e, activity_type, alpha, beta)
        S = cls.calculate_staff_index(N, M, S_y, S_n)
        I = cls.calculate_infrastructure_index(N, F_p, F_t, infra_type)
        
        # 3. Calculate CI
        CI = W_v * V + W_s * S + W_i * I
        
        # 4. Result
        return CrowdIndexResult(
            ci=round(CI, 2),
            v_index=round(V, 2),
            s_index=round(S, 2),
            i_index=round(I, 2),
            level=cls.get_crowd_level(CI)
        )

if __name__ == '__main__':
    # Test case based on example values
    result = CrowdIndexCalculator.calculate(
        spot_type=SpotType.COMPREHENSIVE,
        infra_type=InfraSpotType.COMPREHENSIVE,
        activity_type=ActivityType.STATIC,
        N=5000,         # 5000 visitors
        S_e=50000.0,    # 50000 m^2
        M=100,          # 100 staff
        F_p=4000,       # Public infra capacity 4000
        F_t=4500,       # Tourist infra capacity 4500
        alpha=1.2,      # Local crowding
        beta=1.2        # Peak hour
    )
    
    print(f"CI: {result.ci} ({result.level})")
    print(f"V: {result.v_index}, S: {result.s_index}, I: {result.i_index}")
