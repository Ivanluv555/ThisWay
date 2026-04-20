import json
import logging
import random
import time
from typing import Any, Dict, List, Optional

from app import config
from app.services.district_lookup_service import resolve_district_context
from app.spider.xc_spot_detail import xc_fetch_detail

logger = logging.getLogger(__name__)

DETAIL_FETCH_RETRY_COUNT = 2


def sync_region_spot_details(keyword: str) -> Dict[str, Any]:
    """同步指定地区的景点详情，适合通过 FastAPI BackgroundTasks 异步触发。"""
    ready_info = _check_mysql_ready()
    district_info = _resolve_district_context(keyword)
    spot_rows = _load_spots_by_district(district_info["district_id"])

    if not spot_rows:
        logger.info(
            "地区[%s] (district_id=%s) 在基础表中没有可同步的景点",
            keyword,
            district_info["district_id"],
        )
        return {
            "keyword": keyword,
            "districtId": district_info["district_id"],
            "districtName": district_info.get("district_name", ""),
            "spotCount": 0,
            "updatedCount": 0,
            "failedCount": 0,
            "note": "基础表中没有该地区的景点记录。",
            "mysql": ready_info,
        }

    logger.info(
        "开始同步地区[%s]详情，district_id=%s，基础表命中 %d 条景点",
        keyword,
        district_info["district_id"],
        len(spot_rows),
    )

    updated_count = 0
    failed_count = 0
    processed_ids: List[int] = []

    try:
        import pymysql
    except Exception as exc:
        raise RuntimeError(f"pymysql 未安装，请先安装依赖: {exc}")

    conn = pymysql.connect(
        host=config.MYSQL_HOST,
        port=config.MYSQL_PORT,
        user=config.MYSQL_USER,
        password=config.MYSQL_PASSWORD,
        database=config.MYSQL_DATABASE,
        charset="utf8mb4",
        autocommit=False,
    )

    try:
        with conn.cursor() as cursor:
            for index, spot_row in enumerate(spot_rows, start=1):
                sight_id = int(spot_row["sight_id"])
                try:
                    detail = _fetch_detail_with_retry(district_info["district_id"], sight_id)
                    record = _normalize_detail_record(spot_row, detail, keyword)
                    _upsert_detail_record(cursor, record)
                    conn.commit()
                    updated_count += 1
                    processed_ids.append(sight_id)
                    logger.info(
                        "[%s] 已同步详情 %d/%d: sight_id=%s",
                        keyword,
                        index,
                        len(spot_rows),
                        sight_id,
                    )
                except Exception as exc:
                    conn.rollback()
                    failed_count += 1
                    logger.warning(
                        "[%s] 同步详情失败 sight_id=%s, district_id=%s: %s",
                        keyword,
                        sight_id,
                        district_info["district_id"],
                        exc,
                    )

                _sleep_with_jitter()
    finally:
        conn.close()

    return {
        "keyword": keyword,
        "districtId": district_info["district_id"],
        "districtName": district_info.get("district_name", ""),
        "spotCount": len(spot_rows),
        "updatedCount": updated_count,
        "failedCount": failed_count,
        "processedSightIds": processed_ids,
        "mysql": ready_info,
    }


def check_spot_detail_sync_ready() -> Dict[str, Any]:
    """在任务入队前检查数据库与目标表是否可用。"""
    return _check_mysql_ready()


def _load_spots_by_district(district_id: int) -> List[Dict[str, Any]]:
    try:
        import pymysql
    except Exception as exc:
        raise RuntimeError(f"pymysql 未安装，请先安装依赖: {exc}")

    conn = pymysql.connect(
        host=config.MYSQL_HOST,
        port=config.MYSQL_PORT,
        user=config.MYSQL_USER,
        password=config.MYSQL_PASSWORD,
        database=config.MYSQL_DATABASE,
        charset="utf8mb4",
        autocommit=True,
    )

    table = config.MYSQL_TABLE_SPIDER_SPOTS
    try:
        with conn.cursor() as cursor:
            cursor.execute(
                f"SELECT sight_id, district_id, keyword, code, name, url, image_url, district_name, e_name, comment_score, comment_count "
                f"FROM {table} WHERE district_id=%s ORDER BY id ASC",
                (district_id,),
            )
            rows = cursor.fetchall()
    finally:
        conn.close()

    return [
        {
            "sight_id": int(row[0]),
            "district_id": int(row[1]),
            "keyword": row[2],
            "code": row[3],
            "name": row[4],
            "url": row[5],
            "image_url": row[6],
            "district_name": row[7],
            "e_name": row[8],
            "comment_score": row[9],
            "comment_count": row[10],
        }
        for row in rows
    ]


def _fetch_detail_with_retry(district_id: int, sight_id: int) -> Dict[str, Any]:
    last_error: Optional[Exception] = None
    for attempt in range(DETAIL_FETCH_RETRY_COUNT + 1):
        try:
            return xc_fetch_detail(district_id, sight_id)
        except Exception as exc:
            last_error = exc
            if attempt < DETAIL_FETCH_RETRY_COUNT:
                _sleep_with_jitter(short=True)
    raise RuntimeError(f"详情抓取失败 sight_id={sight_id}, district_id={district_id}: {last_error}")


def _normalize_detail_record(spot_row: Dict[str, Any], detail: Dict[str, Any], keyword: str) -> Dict[str, Any]:
    spot = detail.get("spot", {}) if isinstance(detail, dict) else {}
    base_info = detail.get("base_info", {}) if isinstance(detail, dict) else {}
    images = detail.get("images", {}) if isinstance(detail, dict) else {}
    modules = detail.get("detail_modules", {}) if isinstance(detail, dict) else {}

    return {
        "sight_id": int(spot_row["sight_id"]),
        "district_id": int(spot_row["district_id"]),
        "spot_name": _safe_str(spot.get("name")),
        "page_title": _safe_str(spot.get("page_title")),
        "description": _safe_str(spot.get("description")),
        "keywords": _safe_str(spot.get("keywords")),
        "heat_score": _parse_float(spot.get("heat_score")),
        "review_score": _safe_str(spot.get("review_score")),
        "review_count": _safe_str(spot.get("review_count")),
        "rank_text": _safe_str(spot.get("rank")),
        "address": _safe_str(base_info.get("地址")),
        "open_time": _safe_str(base_info.get("开放时间")),
        "image_origin_urls": json.dumps(images.get("origin", []) or [], ensure_ascii=False),
        "image_thumbnail_urls": json.dumps(images.get("thumbnail", []) or [], ensure_ascii=False),
        "image_declared_total": _parse_int(images.get("declared_total")),
        "image_fetched_preview_count": _parse_int(images.get("fetched_preview_count")),
        "detail_modules_json": json.dumps(modules or {}, ensure_ascii=False),
        "source_keyword": keyword,
    }


def _upsert_detail_record(cursor: Any, record: Dict[str, Any]) -> None:
    table = config.MYSQL_TABLE_SPIDER_SPOT_DETAILS
    sql = (
        f"INSERT INTO {table} ("
        "sight_id, district_id, source_keyword, spot_name, page_title, description, keywords, "
        "heat_score, review_score, review_count, rank_text, address, open_time, "
        "image_origin_urls, image_thumbnail_urls, image_declared_total, image_fetched_preview_count, detail_modules_json"
        ") VALUES ("
        "%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s"
        ") ON DUPLICATE KEY UPDATE "
        "source_keyword=VALUES(source_keyword), "
        "spot_name=VALUES(spot_name), "
        "page_title=VALUES(page_title), "
        "description=VALUES(description), "
        "keywords=VALUES(keywords), "
        "heat_score=VALUES(heat_score), "
        "review_score=VALUES(review_score), "
        "review_count=VALUES(review_count), "
        "rank_text=VALUES(rank_text), "
        "address=VALUES(address), "
        "open_time=VALUES(open_time), "
        "image_origin_urls=VALUES(image_origin_urls), "
        "image_thumbnail_urls=VALUES(image_thumbnail_urls), "
        "image_declared_total=VALUES(image_declared_total), "
        "image_fetched_preview_count=VALUES(image_fetched_preview_count), "
        "detail_modules_json=VALUES(detail_modules_json), "
        "updated_at=CURRENT_TIMESTAMP"
    )
    params = (
        record["sight_id"],
        record["district_id"],
        record["source_keyword"],
        record["spot_name"],
        record["page_title"],
        record["description"],
        record["keywords"],
        record["heat_score"],
        record["review_score"],
        record["review_count"],
        record["rank_text"],
        record["address"],
        record["open_time"],
        record["image_origin_urls"],
        record["image_thumbnail_urls"],
        record["image_declared_total"],
        record["image_fetched_preview_count"],
        record["detail_modules_json"],
    )
    cursor.execute(sql, params)


def _check_mysql_ready() -> Dict[str, Any]:
    try:
        import pymysql
    except Exception as exc:
        raise RuntimeError(f"pymysql 未安装，请先安装依赖: {exc}")

    detail_table = config.MYSQL_TABLE_SPIDER_SPOT_DETAILS
    spot_table = config.MYSQL_TABLE_SPIDER_SPOTS
    conn = pymysql.connect(
        host=config.MYSQL_HOST,
        port=config.MYSQL_PORT,
        user=config.MYSQL_USER,
        password=config.MYSQL_PASSWORD,
        database=config.MYSQL_DATABASE,
        charset="utf8mb4",
        autocommit=True,
    )
    try:
        with conn.cursor() as cursor:
            cursor.execute("SELECT 1")
            cursor.fetchone()
            cursor.execute("SHOW TABLES LIKE %s", (spot_table,))
            if not cursor.fetchone():
                raise RuntimeError(f"基础景点表不存在: {spot_table}")
            cursor.execute("SHOW TABLES LIKE %s", (detail_table,))
            if not cursor.fetchone():
                raise RuntimeError(f"详情表不存在: {detail_table}")
    finally:
        conn.close()

    return {
        "host": config.MYSQL_HOST,
        "port": config.MYSQL_PORT,
        "database": config.MYSQL_DATABASE,
        "spotTable": spot_table,
        "detailTable": detail_table,
        "connected": True,
    }


def _sleep_with_jitter(short: bool = False) -> None:
    base_delay = min(config.SPOT_DETAIL_SYNC_DELAY_SECONDS, 0.5) if short else config.SPOT_DETAIL_SYNC_DELAY_SECONDS
    jitter = min(config.SPOT_DETAIL_SYNC_JITTER_SECONDS, 0.2) if short else config.SPOT_DETAIL_SYNC_JITTER_SECONDS
    delay = base_delay + random.uniform(0.0, jitter)
    time.sleep(max(0.0, delay))


def _safe_str(value: Any) -> str:
    if value is None:
        return ""
    return str(value)


def _parse_float(value: Any) -> Optional[float]:
    if value is None:
        return None
    text = str(value).strip()
    if not text:
        return None
    try:
        return float(text)
    except Exception:
        match = re.search(r"(\d+(?:\.\d+)?)", text)
        if match:
            return float(match.group(1))
        return None


def _parse_int(value: Any) -> Optional[int]:
    if value is None:
        return None
    text = str(value).strip()
    if not text:
        return None
    try:
        return int(float(text))
    except Exception:
        match = re.search(r"(\d+)", text)
        if match:
            return int(match.group(1))
        return None