"""
cidi-model FastAPI 主入口

启动：
    uvicorn main:app --host 0.0.0.0 --port 8001 --reload

环境变量：
    SPRING_BASE_URL   Spring Boot 地址（默认 http://localhost:8080）
    SCENIC_SEED       随机种子（默认 42）
    HISTORY_HOURS     历史追溯小时数（默认 168）
"""
import logging
from datetime import datetime, timedelta
from typing import Optional

import numpy as np
import pandas as pd
from fastapi import FastAPI, HTTPException, Query
from fastapi.middleware.cors import CORSMiddleware

from app import config
from app.models import HourlyFlowRecord, PredictionRecord, PredictResponse
from app.predictor import (
    DiversionPolicy,
    ci_to_level,
    flow_trend,
    generate_mock_history,
    run_diversion_strategy,
    train_and_predict,
)
from app import spring_client

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
logger = logging.getLogger(__name__)

app = FastAPI(
    title="cidi-model 人流预测服务",
    description="通过 Spring Boot API 读取历史数据、写回预测结果，对外暴露预测触发接口。",
    version="1.0.0",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)


# ══════════════════════════════════════════════════════════════════
# 健康检查
# ══════════════════════════════════════════════════════════════════
@app.get("/health", tags=["系统"])
async def health():
    return {"status": "ok", "spring_base": config.SPRING_BASE_URL}


# ══════════════════════════════════════════════════════════════════
# 核心：触发一次完整预测并写回
# ══════════════════════════════════════════════════════════════════
@app.post("/predict", response_model=PredictResponse, tags=["预测"])
async def run_predict(
    hours: int = Query(default=config.HISTORY_HOURS, ge=24, le=720,
                       description="历史数据窗口（小时），不足时自动模拟补齐"),
    write_back: bool = Query(default=True, description="是否将预测结果写回 Spring Boot"),
):
    """
    完整预测流程：
    1. 从 Spring Boot 拉取全量景点列表
    2. 从 Spring Boot 拉取最近 N 小时历史人流（若不足则模拟并回写）
    3. LSTM 训练 + 预测下一小时人流
    4. 运行非强制分流策略
    5. 将预测结果批量写回 Spring Boot
    6. 返回预测摘要
    """
    # 1. 获取景点列表
    try:
        locations = await spring_client.get_locations()
    except Exception as e:
        raise HTTPException(status_code=502, detail=f"无法获取景点列表: {e}")

    if not locations:
        raise HTTPException(status_code=404, detail="数据库中没有景点数据")

    spot_ids = [loc.id for loc in locations]
    title_map = {loc.id: loc.title for loc in locations}
    logger.info("获取到 %d 个景点", len(locations))

    # 2. 拉取历史人流，不足则模拟
    try:
        history_records = await spring_client.get_history(hours=hours)
    except Exception as e:
        logger.warning("拉取历史失败，将全量模拟: %s", e)
        history_records = []

    if len(history_records) < len(spot_ids) * 24:
        logger.info("历史数据不足（共 %d 条），生成 %d 小时模拟数据", len(history_records), hours)
        df_history = generate_mock_history(locations, hours=hours, seed=config.RANDOM_SEED)

        # 将模拟数据写回数据库（建立历史基线）
        if write_back:
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
                logger.info("模拟历史数据已回写 %d 条", len(mock_rows))
            except Exception as e:
                logger.warning("回写模拟历史失败（非致命）: %s", e)
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

    # 3. LSTM 预测
    logger.info("开始训练 LSTM 模型（epochs=60）...")
    try:
        predicted_flow = train_and_predict(df_history, spot_ids, seed=config.RANDOM_SEED)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"模型训练/预测失败: {e}")
    logger.info("预测完成，Top3: %s",
                sorted(predicted_flow.items(), key=lambda x: x[1], reverse=True)[:3])

    # 4. 估算承载量 & 分流策略
    capacity_map = {
        sid: max(float(np.percentile(df_history[sid].values, 95)) * 1.15,
                 float(df_history[sid].mean()) * 1.40, 3000.0)
        for sid in spot_ids
    }
    flow_ba, pairs = run_diversion_strategy(locations, predicted_flow, capacity_map, DiversionPolicy())

    # 5. 组装预测记录
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

    # 6. 写回预测结果
    if write_back:
        try:
            await spring_client.post_predictions_batch(prediction_records)
        except Exception as e:
            logger.warning("写回预测失败（非致命）: %s", e)

    # 7. 汇总摘要
    ci_map_before = {r.spotId: r.ciBefore for r in flow_ba}
    ci_map_after  = {r.spotId: r.ciAfter  for r in flow_ba}
    summary = {
        "predictTime": predict_time.isoformat(),
        "spotCount":   len(spot_ids),
        "peakCiBefore":  round(max(ci_map_before.values(), default=0.0), 4),
        "peakCiAfter":   round(max(ci_map_after.values(),  default=0.0), 4),
        "overloadedBefore": sum(1 for v in ci_map_before.values() if v > 5.0),
        "overloadedAfter":  sum(1 for v in ci_map_after.values()  if v > 5.0),
    }

    from app.models import DiversionPair
    return PredictResponse(
        predictions=prediction_records,
        flowBeforeAfter=flow_ba,
        pairRecommendations=[
            DiversionPair(
                sourceId=p["sourceId"], sourceTitle=p["sourceTitle"],
                targetId=p["targetId"], targetTitle=p["targetTitle"],
                coupon=p["coupon"], finalProb=p["finalProb"],
                expectedTransferFlow=p["expectedTransferFlow"],
            )
            for p in pairs
        ],
        summary=summary,
    )


# ══════════════════════════════════════════════════════════════════
# 快速查询：当前最新预测
# ══════════════════════════════════════════════════════════════════
@app.get("/predictions/latest", tags=["预测"])
async def get_latest_predictions():
    """直接从 Spring Boot 获取最新一轮预测结果（用于前端展示）"""
    try:
        resp = await spring_client._client.get("/api/spot-traffic-predictions/latest")
        resp.raise_for_status()
        return resp.json()
    except Exception as e:
        raise HTTPException(status_code=502, detail=f"获取最新预测失败: {e}")

# ══════════════════════════════════════════════════════════════════
# 接收参数，完成指定景区的拥挤指数计算（正在开发中...）
# ══════════════════════════════════════════════════════════════════
@app.get("/crowd-index", tags=["拥挤指数"])
async def calculate_crowd_index():
    
    return {"message": "拥挤指数计算接口正在开发中，敬请期待！"}
