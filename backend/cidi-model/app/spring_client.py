"""cidi-model — Spring Boot HTTP 客户端（通过 Spring Boot API 读写数据库）"""
import logging
from datetime import datetime
from typing import List, Optional

import httpx

from app import config
from app.models import HourlyFlowRecord, LocationInfo, PredictionRecord

logger = logging.getLogger(__name__)

_BASE = config.SPRING_BASE_URL.rstrip("/")

# httpx 客户端（长连接复用）
_client = httpx.AsyncClient(base_url=_BASE, timeout=30.0)


async def get_locations() -> List[LocationInfo]:
    """GET /api/locations → 返回全部 21 个景点"""
    resp = await _client.get("/api/locations")
    resp.raise_for_status()
    return [LocationInfo(**item) for item in resp.json()]


async def get_history(hours: int = 168, location_id: Optional[int] = None) -> List[HourlyFlowRecord]:
    """GET /api/flow/history → 拉取最近 N 小时所有景点的历史人流"""
    params = {"hours": hours}
    if location_id is not None:
        params["locationId"] = location_id
    resp = await _client.get("/api/flow/history", params=params)
    resp.raise_for_status()
    return [HourlyFlowRecord(**item) for item in resp.json()]


async def post_history_batch(records: List[HourlyFlowRecord]) -> None:
    """POST /api/flow/history/batch → 批量写入模拟/实测历史人流（每次 21 条）"""
    payload = [r.model_dump(mode="json") for r in records]
    resp = await _client.post("/api/flow/history/batch", json=payload)
    resp.raise_for_status()
    logger.info("写入历史人流 %d 条", len(records))


async def post_predictions_batch(records: List[PredictionRecord]) -> None:
    """POST /api/spot-traffic-predictions/batch → 批量写入预测结果"""
    now_str = datetime.now().isoformat()
    payload = []
    for r in records:
        d = r.model_dump(mode="json")
        if d.get("createTime") is None:
            d["createTime"] = now_str
        payload.append(d)
    resp = await _client.post("/api/spot-traffic-predictions/batch", json=payload)
    resp.raise_for_status()
    logger.info("写入预测结果 %d 条", len(records))


async def post_transitions_batch(records) -> None:
    """POST /api/location-transitions/batch → 批量写入转移预测"""
    from app.models import TransitionRecord
    payload = []
    for r in records:
        if isinstance(r, TransitionRecord):
            d = r.model_dump(mode="json")
        else:
            d = r
        payload.append(d)
    resp = await _client.post("/api/location-transitions/batch", json=payload)
    resp.raise_for_status()
    logger.info("写入转移预测 %d 条", len(records))


async def get_latest_flows(location_id: Optional[int] = None) -> List[dict]:
    """GET /api/flow/latest → 获取所有景点的最新人流数据

    返回格式:
    [
        {
            "location_id": 1,
            "title": "洪崖洞",
            "visitor_count": 1500,
            "record_hour": "2026-05-05T14:00:00"
        },
        ...
    ]
    """
    params = {}
    if location_id is not None:
        params["locationId"] = location_id
    resp = await _client.get("/api/flow/latest", params=params)
    resp.raise_for_status()
    return resp.json()
