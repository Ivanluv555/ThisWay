from __future__ import annotations

import asyncio
import logging
import random
from copy import deepcopy
from datetime import datetime
from typing import Any, Dict, List, Optional, Sequence, Tuple

from fastapi.concurrency import run_in_threadpool

from app import config
from app.services.crowd_index_service import compute_batch_congestion_index

logger = logging.getLogger(__name__)

SIMULATION_INTERVAL_SECONDS = 60
SIMULATION_VISITOR_MIN = 100
SIMULATION_VISITOR_MAX = 3000

_TABLE_LOCATIONS = "locations"
_TABLE_LOCATION_HOURLY_FLOW = "location_hourly_flow"


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


def _connect_mysql():
    try:
        import pymysql
    except Exception as exc:
        raise RuntimeError(f"pymysql 未安装，请先安装依赖: {exc}")

    return pymysql.connect(
        host=config.MYSQL_HOST,
        port=config.MYSQL_PORT,
        user=config.MYSQL_USER,
        password=config.MYSQL_PASSWORD,
        database=config.MYSQL_DATABASE,
        charset="utf8mb4",
        autocommit=False,
    )


def _check_mysql_ready_sync() -> Dict[str, Any]:
    conn = _connect_mysql()
    try:
        with conn.cursor() as cursor:
            cursor.execute("SELECT 1")
            cursor.fetchone()
            for table in (_TABLE_LOCATIONS, _TABLE_LOCATION_HOURLY_FLOW):
                cursor.execute("SHOW TABLES LIKE %s", (table,))
                if not cursor.fetchone():
                    raise RuntimeError(f"目标表不存在: {table}")
    finally:
        conn.close()

    return {
        "host": config.MYSQL_HOST,
        "port": config.MYSQL_PORT,
        "database": config.MYSQL_DATABASE,
        "tables": [_TABLE_LOCATIONS, _TABLE_LOCATION_HOURLY_FLOW],
        "connected": True,
    }


def _get_all_locations_sync() -> List[Dict[str, Any]]:
    conn = _connect_mysql()
    try:
        with conn.cursor() as cursor:
            cursor.execute(
                f"SELECT id, title, latitude, longitude, address, category FROM {_TABLE_LOCATIONS} ORDER BY id ASC"
            )
            rows = cursor.fetchall()
    finally:
        conn.close()

    return [
        {
            "id": int(row[0]),
            "title": row[1] or "",
            "latitude": float(row[2]),
            "longitude": float(row[3]),
            "address": row[4] or "",
            "category": row[5],
        }
        for row in rows
    ]


def _get_latest_location_flows_sync(location_id: Optional[int] = None) -> List[Dict[str, Any]]:
    conn = _connect_mysql()
    try:
        with conn.cursor() as cursor:
            if location_id is None:
                cursor.execute(
                    f"SELECT l.id, l.title, h.visitor_count, h.record_hour "
                    f"FROM {_TABLE_LOCATIONS} l "
                    f"JOIN ("
                    f"  SELECT location_id, MAX(record_hour) AS max_hour "
                    f"  FROM {_TABLE_LOCATION_HOURLY_FLOW} GROUP BY location_id"
                    f") last_h ON last_h.location_id = l.id "
                    f"JOIN {_TABLE_LOCATION_HOURLY_FLOW} h "
                    f"  ON h.location_id = last_h.location_id AND h.record_hour = last_h.max_hour "
                    f"ORDER BY l.id ASC"
                )
            else:
                cursor.execute(
                    f"SELECT l.id, l.title, h.visitor_count, h.record_hour "
                    f"FROM {_TABLE_LOCATIONS} l "
                    f"JOIN {_TABLE_LOCATION_HOURLY_FLOW} h ON h.location_id=l.id "
                    f"WHERE l.id=%s "
                    f"ORDER BY h.record_hour DESC LIMIT 1",
                    (location_id,),
                )
            rows = cursor.fetchall()
    finally:
        conn.close()

    return [
        {
            "location_id": int(row[0]),
            "title": row[1] or "",
            "visitor_count": int(row[2]),
            "record_hour": row[3],
        }
        for row in rows
    ]


def _upsert_location_hourly_flow_sync(records: Sequence[Tuple[int, datetime, int, float, str]]) -> Dict[str, Any]:
    if not records:
        return {"inserted": 0, "skipped": 0}

    conn = _connect_mysql()
    try:
        with conn.cursor() as cursor:
            sql = (
                f"INSERT IGNORE INTO {_TABLE_LOCATION_HOURLY_FLOW} ("
                "location_id, record_hour, visitor_count, congestion_index, data_source"
                ") VALUES (%s, %s, %s, %s, %s)"
            )
            cursor.executemany(sql, records)
            inserted_count = int(cursor.rowcount)
        conn.commit()
    except Exception:
        conn.rollback()
        raise
    finally:
        conn.close()

    return {
        "inserted": inserted_count,
        "skipped": max(0, len(records) - inserted_count),
    }


def _run_single_tick_sync() -> Dict[str, Any]:
    locations = _get_all_locations_sync()
    if not locations:
        raise RuntimeError("locations 表中没有可用景点数据")

    record_hour = datetime.now().replace(minute=0, second=0, microsecond=0)
    visitor_map = {
        int(loc["id"]): random.randint(SIMULATION_VISITOR_MIN, SIMULATION_VISITOR_MAX)
        for loc in locations
    }
    ci_map = compute_batch_congestion_index(visitor_map)

    rows = [
        (
            location_id,
            record_hour,
            visitor_map[location_id],
            ci_map[location_id],
            "simulated",
        )
        for location_id in visitor_map
    ]

    write_result = _upsert_location_hourly_flow_sync(rows)
    return {
        "recordHour": record_hour.isoformat(),
        "spotCount": len(locations),
        "inserted": write_result["inserted"],
        "skipped": write_result["skipped"],
        "visitorMin": min(visitor_map.values()),
        "visitorMax": max(visitor_map.values()),
    }


async def check_mysql_ready() -> Dict[str, Any]:
    return await run_in_threadpool(_check_mysql_ready_sync)


async def get_all_locations() -> List[Dict[str, Any]]:
    return await run_in_threadpool(_get_all_locations_sync)


async def get_all_location_ids() -> List[int]:
    rows = await get_all_locations()
    return [int(row["id"]) for row in rows]


async def run_single_tick() -> Dict[str, Any]:
    return await run_in_threadpool(_run_single_tick_sync)


async def get_latest_location_flows(location_id: Optional[int] = None) -> List[Dict[str, Any]]:
    return await run_in_threadpool(_get_latest_location_flows_sync, location_id)


def get_simulation_status() -> Dict[str, Any]:
    return deepcopy(_state)


async def start_simulation() -> Dict[str, Any]:
    global _worker_task, _stop_event

    if _state["running"] and _worker_task and not _worker_task.done():
        return get_simulation_status()

    await check_mysql_ready()

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
