"""
flow_simulation_service.py

模拟人流数据生成服务

重要：本服务通过 Spring Boot API 访问数据库，不直接连接 MySQL
"""
from __future__ import annotations

import asyncio
import logging
import random
from copy import deepcopy
from datetime import datetime, timedelta
from typing import Any, Dict, List, Optional, Sequence, Tuple

from fastapi.concurrency import run_in_threadpool

from app import config, spring_client
from app.models import HourlyFlowRecord, LocationInfo
from app.services.crowd_index_service import compute_batch_congestion_index
from app.services.visitor_profile_service import (
    initialize_spot_profiles,
    generate_all_visitor_counts,
    reset_runtime_state,
)

logger = logging.getLogger(__name__)

SIMULATION_INTERVAL_SECONDS = 60
SIMULATION_VISITOR_MIN = 100
SIMULATION_VISITOR_MAX = 3000


_state: Dict[str, Any] = {
    "running": False,
    "stopping": False,
    "startedAt": None,
    "lastRunAt": None,
    "lastError": None,
    "tickCount": 0,
    "lastSummary": None,
    "intervalSeconds": SIMULATION_INTERVAL_SECONDS,
    "visitorMin": SIMULATION_VISITOR_MIN,
    "visitorMax": SIMULATION_VISITOR_MAX,
}

_stop_event: Optional[asyncio.Event] = None
_worker_task: Optional[asyncio.Task] = None


# ══════════════════════════════════════════════════════════════════
# 数据访问层：通过 Spring Boot API 访问数据库
# ══════════════════════════════════════════════════════════════════

async def _get_all_locations() -> List[LocationInfo]:
    """通过 Spring Boot API 获取所有景点"""
    return await spring_client.get_locations()


async def _get_latest_location_flows(location_id: Optional[int] = None) -> List[Dict[str, Any]]:
    """通过 Spring Boot API 获取最新人流数据

    返回格式:
    [
        {
            "locationId": 1,
            "recordHour": "2026-05-05T14:00:00",
            "visitorCount": 1500,
            "congestionIndex": 5.2
        },
        ...
    ]
    """
    return await spring_client.get_latest_flows(location_id)


async def _batch_write_flow_records(records: List[HourlyFlowRecord]) -> None:
    """通过 Spring Boot API 批量写入人流记录"""
    await spring_client.post_history_batch(records)


# ══════════════════════════════════════════════════════════════════
# 业务逻辑层
# ══════════════════════════════════════════════════════════════════

async def _run_single_tick() -> Dict[str, Any]:
    """执行一次模拟数据生成"""
    locations = await _get_all_locations()
    if not locations:
        raise RuntimeError("没有可用景点数据")

    record_hour = datetime.now().replace(minute=0, second=0, microsecond=0)

    # 转换为字典格式供 visitor_profile_service 使用
    location_dicts = [
        {
            "id": loc.id,
            "title": loc.title,
            "latitude": loc.latitude,
            "longitude": loc.longitude,
            "address": loc.address,
            "category": loc.category,
        }
        for loc in locations
    ]

    # 使用基于景点特征的智能访客生成
    visitor_map = generate_all_visitor_counts(location_dicts, current_time=record_hour)
    ci_map = compute_batch_congestion_index(visitor_map)

    # 构建记录
    records = [
        HourlyFlowRecord(
            locationId=location_id,
            recordHour=record_hour,
            visitorCount=visitor_map[location_id],
            congestionIndex=ci_map[location_id],
            dataSource="simulated",
        )
        for location_id in visitor_map
    ]

    # 写入数据库
    await _batch_write_flow_records(records)

    return {
        "recordHour": record_hour.isoformat(),
        "spotCount": len(locations),
        "inserted": len(records),
        "visitorMin": min(visitor_map.values()),
        "visitorMax": max(visitor_map.values()),
    }


async def _generate_and_write_multiple_hours(hours: int, lookback: bool = False) -> Dict[str, Any]:
    """一次性生成并写入指定小时数的模拟数据

    Args:
        hours: 要生成的小时数（正整数，最多 720 即 30 天）
        lookback: 是否生成往回的历史数据；False=生成往未来的数据，True=生成往过去的数据

    Returns:
        包含生成统计信息的字典
    """
    if hours <= 0:
        raise ValueError("hours 必须是正整数")
    if hours > 720:
        raise ValueError("最多一次性生成 720 小时（30 天）的数据")

    locations = await _get_all_locations()
    if not locations:
        raise RuntimeError("没有可用景点数据")

    # 转换为字典格式
    location_dicts = [
        {
            "id": loc.id,
            "title": loc.title,
            "latitude": loc.latitude,
            "longitude": loc.longitude,
            "address": loc.address,
            "category": loc.category,
        }
        for loc in locations
    ]

    # 批量生成前重置时序状态，保证该批次内部的趋势连续且不受历史残留影响
    initialize_spot_profiles(location_dicts, reset_state=True)

    spot_count = len(locations)
    all_records = []

    # 生成时间范围
    now = datetime.now().replace(minute=0, second=0, microsecond=0)

    if lookback:
        # 往过去生成：从 now - hours 到 now
        start_time = now - timedelta(hours=hours)
        time_range = [start_time + timedelta(hours=i) for i in range(hours)]
    else:
        # 往未来生成：从 now + 1 到 now + hours
        time_range = [now + timedelta(hours=i+1) for i in range(hours)]

    # 为每个时间点生成所有景点的数据
    for record_hour in time_range:
        visitor_map = generate_all_visitor_counts(location_dicts, current_time=record_hour)
        ci_map = compute_batch_congestion_index(visitor_map)

        for location_id in visitor_map:
            all_records.append(
                HourlyFlowRecord(
                    locationId=location_id,
                    recordHour=record_hour,
                    visitorCount=visitor_map[location_id],
                    congestionIndex=ci_map[location_id],
                    dataSource="simulated",
                )
            )

    # 一次性写入所有记录
    await _batch_write_flow_records(all_records)

    total_records = len(all_records)
    date_range_desc = f"从 {time_range[0].isoformat()} 到 {time_range[-1].isoformat()}"

    return {
        "totalRecords": total_records,
        "hoursGenerated": hours,
        "spotsCount": spot_count,
        "dateRange": date_range_desc,
        "inserted": total_records,
        "dataSource": "simulated",
    }

# ══════════════════════════════════════════════════════════════════
# 公共接口层（供 FastAPI 路由调用）
# ══════════════════════════════════════════════════════════════════

async def check_spring_boot_ready() -> Dict[str, Any]:
    """检查 Spring Boot 服务是否可用"""
    try:
        locations = await spring_client.get_locations()
        return {
            "springBootUrl": config.SPRING_BASE_URL,
            "connected": True,
            "locationCount": len(locations),
        }
    except Exception as e:
        return {
            "springBootUrl": config.SPRING_BASE_URL,
            "connected": False,
            "error": str(e),
        }


async def get_all_location_ids() -> List[int]:
    """获取所有景点ID"""
    locations = await _get_all_locations()
    return [loc.id for loc in locations]


async def get_latest_location_flows(location_id: Optional[int] = None) -> List[Dict[str, Any]]:
    """获取最新人流数据"""
    return await _get_latest_location_flows(location_id)


async def run_single_tick() -> Dict[str, Any]:
    """执行一次模拟数据生成（异步）"""
    return await _run_single_tick()


async def generate_and_write_multiple_hours(hours: int, lookback: bool = False) -> Dict[str, Any]:
    """批量生成历史数据（异步）"""
    return await _generate_and_write_multiple_hours(hours, lookback)


async def generate_and_write_multiple_hours(hours: int, lookback: bool = False) -> Dict[str, Any]:
    """异步包装：一次性生成并写入指定小时数的模拟数据。"""
    return await run_in_threadpool(_generate_and_write_multiple_hours_sync, hours, lookback)


def get_simulation_status() -> Dict[str, Any]:
    return deepcopy(_state)


async def start_simulation() -> Dict[str, Any]:
    global _worker_task, _stop_event

    if _state["running"] and _worker_task and not _worker_task.done():
        return get_simulation_status()

    await check_mysql_ready()

    # 初始化景点特征缓存（为每个景点生成基础流量）
    locations_for_init = await get_all_locations()
    await run_in_threadpool(initialize_spot_profiles, locations_for_init, True)

    _stop_event = asyncio.Event()
    _state["running"] = True
    _state["stopping"] = False
    _state["startedAt"] = datetime.now().isoformat()
    _state["lastError"] = None

    _worker_task = asyncio.create_task(_simulation_loop(), name="flow-simulation-worker")
    return get_simulation_status()


def stop_simulation() -> Dict[str, Any]:
    if not _state["running"]:
        return get_simulation_status()

    _state["stopping"] = True
    if _stop_event:
        _stop_event.set()
    # 停止后重置时序状态，避免后续任务复用过期噪声/扰动。
    reset_runtime_state()
    return get_simulation_status()


async def _simulation_loop() -> None:
    global _worker_task, _stop_event

    try:
        while True:
            if _stop_event and _stop_event.is_set():
                break

            try:
                summary = await run_single_tick()
                _state["tickCount"] += 1
                _state["lastRunAt"] = datetime.now().isoformat()
                _state["lastSummary"] = summary
                _state["lastError"] = None
                logger.info(
                    "模拟上报完成: tick=%d, recordHour=%s, spots=%d, inserted=%d, skipped=%d",
                    _state["tickCount"],
                    summary.get("recordHour"),
                    int(summary.get("spotCount", 0)),
                    int(summary.get("inserted", 0)),
                    int(summary.get("skipped", 0)),
                )
            except Exception as exc:
                _state["lastError"] = str(exc)
                logger.warning("模拟上报失败: %s", exc)

            if _stop_event and _stop_event.is_set():
                break

            try:
                await asyncio.wait_for(_stop_event.wait(), timeout=SIMULATION_INTERVAL_SECONDS)
            except asyncio.TimeoutError:
                pass
    finally:
        _state["running"] = False
        _state["stopping"] = False
        _worker_task = None
        _stop_event = None
