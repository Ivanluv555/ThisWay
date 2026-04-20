"""
cidi-model FastAPI 主入口

启动：
    cd .\devs\backend\cidi-model
    uvicorn main:app --host 0.0.0.0 --port 8001 --reload

环境变量：
    SPRING_BASE_URL   Spring Boot 地址（默认 http://localhost:8080）
    SCENIC_SEED       随机种子（默认 42）
    HISTORY_HOURS     历史追溯小时数（默认 168）
    MYSQL_HOST        MySQL 地址（默认 127.0.0.1）
    MYSQL_PORT        MySQL 端口（默认 3306）
    MYSQL_USER        MySQL 用户（默认 root）
    MYSQL_PASSWORD    MySQL 密码
    MYSQL_DATABASE    MySQL 库名（默认 cidi_db）
    MYSQL_TABLE_SPIDER_SPOTS  spider 景点表名（默认 spider_regional_scenic_spots）
    MYSQL_TABLE_SPIDER_SPOT_DETAILS  spider 景点详情表名（默认 spider_scenic_spot_details）
    SPOT_DETAIL_SYNC_DELAY_SECONDS   详情同步请求间隔秒数（默认 1.5）
    SPOT_DETAIL_SYNC_JITTER_SECONDS  详情同步抖动秒数（默认 0.8）
"""
import logging
from datetime import datetime, timedelta
from typing import Optional

import numpy as np
import pandas as pd
from fastapi import BackgroundTasks, FastAPI, HTTPException, Query
from fastapi.concurrency import run_in_threadpool
from fastapi.middleware.cors import CORSMiddleware

from app import config
from app.models import HourlyFlowRecord, PredictionRecord, PredictResponse
from app.spider.xc_spot_search import xc_fetch_search
from app.spider.xc_spot_detail import xc_fetch_detail
from app.services.spot_detail_sync_service import check_spot_detail_sync_ready, sync_region_spot_details
from app.services.spot_sync_service import crawl_and_prepare_spots
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


# ══════════════════════════════════════════════════════════════════
# 获取所有的景点列表
# 遍历所有的景点，并且更新数据库存储的景点数据，没有则添加，有则更新
# ══════════════════════════════════════════════════════════════════
@app.get("/spots/all", tags=["数据采集"])
async def crawl_all_spots(
    keyword: str = Query(default="重庆", min_length=1, description="待遍历地点，例如：重庆"),
):
    """遍历指定地点的全部景点并打印结果；MySQL 写入逻辑暂留空。"""
    return await crawl_and_prepare_spots(keyword)


@app.post("/spots/sync", tags=["数据采集"])
async def sync_spots(
    keyword: str = Query(..., min_length=1, description="待同步地点，例如：重庆"),
):
    """同步入口：参数仅地点字符串。分页由服务内部控制，不对外暴露。"""
    return await crawl_and_prepare_spots(keyword)


@app.post("/spots/details/sync", tags=["数据采集"], status_code=202)
async def sync_spot_details(
    background_tasks: BackgroundTasks,
    keyword: str = Query(..., min_length=1, description="待同步地区，例如：重庆"),
):
    """异步同步指定地区的景点详情，立即返回任务已受理。"""
    try:
        await run_in_threadpool(check_spot_detail_sync_ready)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"景点详情同步前置检查失败: {e}")

    background_tasks.add_task(sync_region_spot_details, keyword)
    return {
        "message": f"已接收 {keyword} 地区景点详情同步任务，后台将异步执行。",
        "keyword": keyword,
        "status": "accepted",
    }



# ══════════════════════════════════════════════════════════════════
# 搜索地区的景区列表
# ══════════════════════════════════════════════════════════════════
@app.get("/spots", tags=["数据采集"])
async def search_spots(
    keyword: str = Query(default="重庆", min_length=1, description="搜索关键词"),
    pageIndex: Optional[int] = Query(default=None, ge=1, alias="pageIndex", description="页码，从 1 开始"),
    pageSize: Optional[int] = Query(default=None, ge=1, le=50, alias="pageSize", description="每页条数"),
    page_index: Optional[int] = Query(default=None, ge=1, alias="page_index", description="页码，从 1 开始（兼容下划线命名）"),
    page_size: Optional[int] = Query(default=None, ge=1, le=50, alias="page_size", description="每页条数（兼容下划线命名）"),
):
    """通过携程景点搜索接口查询景区数据。"""

    effective_page_index = page_index if page_index is not None else (pageIndex if pageIndex is not None else 1)
    effective_page_size = page_size if page_size is not None else (pageSize if pageSize is not None else 12)

    try:
        result = await run_in_threadpool(xc_fetch_search, keyword, effective_page_index, effective_page_size)
    except Exception as e:
        raise HTTPException(status_code=502, detail=f"查询景区数据失败: {e}")

    return {
        "keyword": keyword,
        "pageIndex": effective_page_index,
        "pageSize": effective_page_size,
        **result,
    }

# 重庆 district_code 158
# ══════════════════════════════════════════════════════════════════
# 查询指定景区的详细数据
# ══════════════════════════════════════════════════════════════════
@app.get("/spots/{district_code}/{sight_code}", tags=["数据采集"])
async def get_spot_detail(district_code: int, sight_code: int):
    """通过携程景点详情页查询指定景区详细数据（含介绍、政策、设施、问答、点评与图片）。"""
    try:
        detail = await run_in_threadpool(xc_fetch_detail, district_code, sight_code)
    except Exception as e:
        raise HTTPException(status_code=502, detail=f"查询景区详情失败: {e}")

    spot_name = detail.get("spot", {}).get("name", "") if isinstance(detail, dict) else ""
    if not spot_name:
        raise HTTPException(status_code=404, detail="未找到景区详情，可能景点不存在或页面结构已变化")

    return {
        "district_code": district_code,
        "sight_code": sight_code,
        **detail,
    }

