"""
prediction_scheduler_service.py

定时预测服务：每整点自动触发人流预测并写回数据库

功能：
- 每小时整点自动触发预测
- 从 Spring Boot API 获取历史数据
- 运行 LSTM 预测模型
- 计算分流策略
- 将预测结果写回 Spring Boot API
"""
from __future__ import annotations

import asyncio
import logging
from copy import deepcopy
from datetime import datetime, timedelta
from typing import Any, Dict, Optional

import numpy as np
import pandas as pd
from fastapi.concurrency import run_in_threadpool

from app import config, spring_client
from app.models import HourlyFlowRecord, PredictionRecord, TransitionRecord
from app.predictor import (
    DiversionPolicy,
    ci_to_level,
    flow_trend,
    generate_mock_history,
    run_diversion_strategy,
    train_and_predict,
)

logger = logging.getLogger(__name__)

# 配置
PREDICTION_CHECK_INTERVAL_SECONDS = 30  # 每30秒检查一次是否到整点
HISTORY_HOURS = config.HISTORY_HOURS  # 历史数据窗口


_state: Dict[str, Any] = {
    "running": False,
    "stopping": False,
    "startedAt": None,
    "lastPredictAt": None,
    "lastError": None,
    "predictionCount": 0,
    "lastSummary": None,
    "nextScheduledHour": None,
}

_stop_event: Optional[asyncio.Event] = None
_worker_task: Optional[asyncio.Task] = None


# ══════════════════════════════════════════════════════════════════
# 核心预测逻辑
# ══════════════════════════════════════════════════════════════════

async def _run_hourly_prediction() -> Dict[str, Any]:
    """执行一次完整的预测流程"""
    logger.info("开始执行定时预测...")

    # 1. 获取景点列表
    try:
        locations = await spring_client.get_locations()
    except Exception as e:
        raise RuntimeError(f"无法获取景点列表: {e}")

    if not locations:
        raise RuntimeError("数据库中没有景点数据")

    spot_ids = [loc.id for loc in locations]
    title_map = {loc.id: loc.title for loc in locations}
    logger.info(f"获取到 {len(locations)} 个景点")

    # 2. 拉取历史人流数据
    try:
        history_records = await spring_client.get_history(hours=HISTORY_HOURS)
    except Exception as e:
        logger.warning(f"拉取历史失败，将全量模拟: {e}")
        history_records = []

    # 3. 数据准备：不足则模拟补齐
    if len(history_records) < len(spot_ids) * 24:
        logger.info(f"历史数据不足（共 {len(history_records)} 条），生成 {HISTORY_HOURS} 小时模拟数据")
        df_history = await run_in_threadpool(
            generate_mock_history, locations, hours=HISTORY_HOURS, seed=config.RANDOM_SEED
        )

        # 将模拟数据写回数据库（建立历史基线）
        mock_rows = []
        for ts, row in df_history.iterrows():
            for loc in locations:
                mock_rows.append(HourlyFlowRecord(
                    locationId=loc.id,
                    recordHour=ts.to_pydatetime(),
                    visitorCount=int(row[loc.id]),
                    congestionIndex=None,
                    dataSource="simulated",
                ))
        try:
            await spring_client.post_history_batch(mock_rows)
            logger.info(f"模拟历史数据已回写 {len(mock_rows)} 条")
        except Exception as e:
            logger.warning(f"回写模拟历史失败（非致命）: {e}")
    else:
        # 将已有记录重建为 DataFrame
        time_set = sorted({r.recordHour for r in history_records})
        df_history = pd.DataFrame(
            index=pd.DatetimeIndex(time_set),
            columns=spot_ids,
            dtype=float,
        )
        for r in history_records:
            if r.recordHour in df_history.index and r.locationId in df_history.columns:
                df_history.loc[r.recordHour, r.locationId] = float(r.visitorCount)
        df_history.ffill(inplace=True)
        df_history.fillna(0.0, inplace=True)

    # 确保列顺序与 spot_ids 一致
    df_history = df_history.reindex(columns=spot_ids, fill_value=0.0)
    history_means = {sid: float(df_history[sid].mean()) for sid in spot_ids}

    # 4. LSTM 预测
    logger.info("开始训练 LSTM 模型...")
    try:
        predicted_flow = await run_in_threadpool(
            train_and_predict, df_history, spot_ids, seed=config.RANDOM_SEED
        )
    except Exception as e:
        raise RuntimeError(f"模型训练/预测失败: {e}")

    logger.info(f"预测完成，Top3: {sorted(predicted_flow.items(), key=lambda x: x[1], reverse=True)[:3]}")

    # 5. 估算承载量 & 分流策略
    capacity_map = {
        sid: max(
            float(np.percentile(df_history[sid].values, 95)) * 1.15,
            float(df_history[sid].mean()) * 1.40,
            3000.0
        )
        for sid in spot_ids
    }

    flow_ba, pairs = await run_in_threadpool(
        run_diversion_strategy, locations, predicted_flow, capacity_map, DiversionPolicy()
    )

    # 6. 组装预测记录
    predict_time = (datetime.now() + timedelta(hours=1)).replace(minute=0, second=0, microsecond=0)
    prediction_records = []
    for sid in spot_ids:
        pf = predicted_flow[sid]
        cap = capacity_map[sid]
        ci = min(10.0, 10.0 * pf / max(cap, 1e-6))
        prediction_records.append(PredictionRecord(
            spotId=sid,
            predictTime=predict_time,
            visitorCount=int(pf),
            congestionLevel=ci_to_level(ci),
            trend=flow_trend(pf, history_means[sid]),
            trafficLevel=ci_to_level(ci),
            crowdDensity=round(ci, 2),
            createTime=datetime.now(),
        ))

    # 7. 写回预测结果
    try:
        await spring_client.post_predictions_batch(prediction_records)
        logger.info(f"写入预测结果 {len(prediction_records)} 条")

        # 写回转移预测
        transition_records = []
        for pair in pairs:
            transition_records.append(TransitionRecord(
                sourceLocationId=pair["sourceId"],
                targetLocationId=pair["targetId"],
                predictHour=predict_time,
                transitionProbability=pair["finalProb"],
                expectedUsers=int(pair["expectedTransferFlow"]),
                couponAmount=pair["coupon"],
                createTime=datetime.now(),
            ))

        if transition_records:
            await spring_client.post_transitions_batch(transition_records)
            logger.info(f"写入转移预测 {len(transition_records)} 条")

    except Exception as e:
        logger.warning(f"写回预测失败（非致命）: {e}")

    # 8. 汇总摘要
    ci_map_before = {r.spotId: r.ciBefore for r in flow_ba}
    ci_map_after = {r.spotId: r.ciAfter for r in flow_ba}

    summary = {
        "predictTime": predict_time.isoformat(),
        "spotCount": len(spot_ids),
        "peakCiBefore": round(max(ci_map_before.values(), default=0.0), 4),
        "peakCiAfter": round(max(ci_map_after.values(), default=0.0), 4),
        "overloadedBefore": sum(1 for v in ci_map_before.values() if v > 5.0),
        "overloadedAfter": sum(1 for v in ci_map_after.values() if v > 5.0),
        "diversionPairs": len(pairs),
    }

    logger.info(f"定时预测完成: {summary}")
    return summary


# ══════════════════════════════════════════════════════════════════
# 定时调度逻辑
# ══════════════════════════════════════════════════════════════════

def _get_next_hour() -> datetime:
    """获取下一个整点时间"""
    now = datetime.now()
    next_hour = (now + timedelta(hours=1)).replace(minute=0, second=0, microsecond=0)
    return next_hour


def _should_run_prediction() -> bool:
    """判断是否应该执行预测（当前时间是否刚过整点）"""
    now = datetime.now()
    # 如果当前分钟在 0-1 之间，且秒数小于 PREDICTION_CHECK_INTERVAL_SECONDS，认为刚过整点
    if now.minute <= 1 and now.second < PREDICTION_CHECK_INTERVAL_SECONDS * 2:
        # 检查是否已经在这个小时执行过
        last_predict = _state.get("lastPredictAt")
        if last_predict:
            last_predict_dt = datetime.fromisoformat(last_predict)
            # 如果上次预测是在当前小时内，则跳过
            if last_predict_dt.hour == now.hour and last_predict_dt.date() == now.date():
                return False
        return True
    return False


async def _prediction_loop() -> None:
    """定时预测主循环"""
    global _worker_task, _stop_event

    try:
        logger.info("定时预测服务已启动，等待整点触发...")
        _state["nextScheduledHour"] = _get_next_hour().isoformat()

        while True:
            if _stop_event and _stop_event.is_set():
                break

            # 检查是否到达整点
            if _should_run_prediction():
                try:
                    logger.info("检测到整点，开始执行预测...")
                    summary = await _run_hourly_prediction()

                    _state["predictionCount"] += 1
                    _state["lastPredictAt"] = datetime.now().isoformat()
                    _state["lastSummary"] = summary
                    _state["lastError"] = None
                    _state["nextScheduledHour"] = _get_next_hour().isoformat()

                    logger.info(
                        f"定时预测完成: count={_state['predictionCount']}, "
                        f"predictTime={summary.get('predictTime')}, "
                        f"spots={summary.get('spotCount')}, "
                        f"diversionPairs={summary.get('diversionPairs')}"
                    )
                except Exception as exc:
                    _state["lastError"] = str(exc)
                    logger.error(f"定时预测失败: {exc}", exc_info=True)

            if _stop_event and _stop_event.is_set():
                break

            # 等待下一次检查
            try:
                await asyncio.wait_for(
                    _stop_event.wait(),
                    timeout=PREDICTION_CHECK_INTERVAL_SECONDS
                )
            except asyncio.TimeoutError:
                pass

    finally:
        _state["running"] = False
        _state["stopping"] = False
        _worker_task = None
        _stop_event = None
        logger.info("定时预测服务已停止")


# ══════════════════════════════════════════════════════════════════
# 公共接口层（供 FastAPI 路由调用）
# ══════════════════════════════════════════════════════════════════

def get_prediction_status() -> Dict[str, Any]:
    """获取定时预测服务状态"""
    return deepcopy(_state)


async def start_prediction_scheduler() -> Dict[str, Any]:
    """启动定时预测服务"""
    global _worker_task, _stop_event

    if _state["running"] and _worker_task and not _worker_task.done():
        logger.info("定时预测服务已在运行中")
        return get_prediction_status()

    # 检查 Spring Boot 连接
    try:
        locations = await spring_client.get_locations()
        if not locations:
            raise RuntimeError("无法获取景点列表")
    except Exception as e:
        raise RuntimeError(f"Spring Boot 服务不可用: {e}")

    _stop_event = asyncio.Event()
    _state["running"] = True
    _state["stopping"] = False
    _state["startedAt"] = datetime.now().isoformat()
    _state["lastError"] = None
    _state["nextScheduledHour"] = _get_next_hour().isoformat()

    _worker_task = asyncio.create_task(_prediction_loop(), name="prediction-scheduler-worker")
    logger.info("定时预测服务已启动")
    return get_prediction_status()


def stop_prediction_scheduler() -> Dict[str, Any]:
    """停止定时预测服务"""
    if not _state["running"]:
        logger.info("定时预测服务未在运行")
        return get_prediction_status()

    _state["stopping"] = True
    if _stop_event:
        _stop_event.set()

    logger.info("正在停止定时预测服务...")
    return get_prediction_status()


async def run_prediction_now() -> Dict[str, Any]:
    """立即执行一次预测（手动触发）"""
    logger.info("手动触发预测...")
    summary = await _run_hourly_prediction()
    return summary
