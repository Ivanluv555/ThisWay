"""cidi-model — Pydantic 数据模型（与 Spring Boot API 的 JSON 契约一致）"""
from datetime import datetime
from typing import List, Optional
from pydantic import BaseModel, Field


# ── 景点基础信息（对应 locations 表） ─────────────────────────────────────────
class LocationInfo(BaseModel):
    id: int
    title: str
    latitude: float
    longitude: float
    address: str = ""
    category: Optional[str] = None


# ── 历史人流（对应 location_hourly_flow 表） ──────────────────────────────────
class HourlyFlowRecord(BaseModel):
    id: Optional[int] = None
    locationId: int
    recordHour: datetime
    visitorCount: int
    congestionIndex: Optional[float] = None
    dataSource: str = "simulated"


# ── 预测结果（对应 spot_traffic_predictions 表） ──────────────────────────────
class PredictionRecord(BaseModel):
    spotId: int
    predictTime: datetime
    visitorCount: int
    congestionLevel: int                    # 1-5 级
    trend: str                              # "上升" / "下降" / "平稳"
    trafficLevel: int                       # 1-5 级
    crowdDensity: Optional[float] = None    # 拥挤指数 CI (0-10)
    createTime: Optional[datetime] = None


# ── 分流推荐明细 ──────────────────────────────────────────────────────────────
class DiversionPair(BaseModel):
    sourceId: int
    sourceTitle: str
    targetId: int
    targetTitle: str
    coupon: float
    finalProb: float
    expectedTransferFlow: float


# ── 分流前后人流对比 ───────────────────────────────────────────────────────────
class FlowBeforeAfter(BaseModel):
    spotId: int
    spotTitle: str
    flowBefore: float
    flowAfter: float
    ciBefore: float
    ciAfter: float


# ── 地区编码查询结果 ──────────────────────────────────────────────────────────
class DistrictLookupResponse(BaseModel):
    keyword: str
    districtId: int
    districtName: str = ""


# ── 拥挤指数查询返回 ───────────────────────────────────────────────────────────
class CrowdIndexItem(BaseModel):
    locationId: int
    locationTitle: str
    visitorCount: int
    congestionIndex: float


class CrowdIndexResponse(BaseModel):
    recordHour: Optional[str] = None
    itemCount: int
    items: List[CrowdIndexItem]


# ── 模拟上报任务状态 ──────────────────────────────────────────────────────────
class SimulationStatusResponse(BaseModel):
    running: bool
    stopping: bool
    startedAt: Optional[str] = None
    lastRunAt: Optional[str] = None
    lastError: Optional[str] = None
    tickCount: int = 0
    lastSummary: Optional[dict] = None
    intervalSeconds: int
    visitorMin: int
    visitorMax: int


# ── 转移预测记录（对应 location_transitions 表） ───────────────────────────
class TransitionRecord(BaseModel):
    sourceLocationId: int
    targetLocationId: int
    predictHour: datetime
    transitionProbability: float
    expectedUsers: int
    couponAmount: float = 0.0
    createTime: Optional[datetime] = None


# ── 预测任务返回结构 ──────────────────────────────────────────────────────────
class PredictResponse(BaseModel):
    predictions: List[PredictionRecord]
    flowBeforeAfter: List[FlowBeforeAfter]
    pairRecommendations: List[DiversionPair]
    summary: dict
