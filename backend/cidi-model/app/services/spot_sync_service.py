import logging
import re
from typing import Dict, List, Optional, Tuple

from fastapi import HTTPException
from fastapi.concurrency import run_in_threadpool

from app import config
from app.spider.xc_spot_search import xc_fetch_search

SPIDER_DEFAULT_PAGE_SIZE = 20
SPIDER_INTERNAL_MAX_PAGES = 200

logger = logging.getLogger(__name__)


async def crawl_and_prepare_spots(keyword: str) -> Dict:
    try:
        await run_in_threadpool(_check_mysql_ready)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"MySQL 连接检查失败: {e}")

    page_index = 1
    total_reported = 0
    raw_item_count = 0
    bad_row_count = 0
    reached_internal_limit = False
    dedup_map: Dict[Tuple[int, int], Dict] = {}

    while True:
        if page_index > SPIDER_INTERNAL_MAX_PAGES:
            reached_internal_limit = True
            logger.warning("达到内部页数保护阈值 %d，提前停止抓取", SPIDER_INTERNAL_MAX_PAGES)
            break

        try:
            result = await run_in_threadpool(xc_fetch_search, keyword, page_index, SPIDER_DEFAULT_PAGE_SIZE)
        except Exception as e:
            raise HTTPException(status_code=502, detail=f"第 {page_index} 页抓取失败: {e}")

        items = result.get("items", []) or []
        total_reported = max(total_reported, int(result.get("pageCount", 0) or 0))
        logger.info("抓取地点[%s]第 %d 页，返回 %d 条", keyword, page_index, len(items))

        for item in items:
            raw_item_count += 1
            normalized = _normalize_spot_item(keyword, item)
            if normalized is None:
                bad_row_count += 1
                continue
            dedup_map[(normalized["sight_id"], normalized["district_id"])] = normalized

        has_next = bool(result.get("hasNext", False))
        if not has_next:
            break

        if total_reported > 0 and page_index >= total_reported:
            break

        page_index += 1

    spots = list(dedup_map.values())
    try:
        await run_in_threadpool(_sync_spots_to_mysql, spots)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"MySQL 同步失败: {e}")

    return {
        "keyword": keyword,
        "pageSize": SPIDER_DEFAULT_PAGE_SIZE,
        "pagesFetched": page_index if not reached_internal_limit else SPIDER_INTERNAL_MAX_PAGES,
        "totalReported": total_reported,
        "rawItemCount": raw_item_count,
        "dedupedItemCount": len(spots),
        "badRowCount": bad_row_count,
        "internalPageLimit": SPIDER_INTERNAL_MAX_PAGES,
        "reachedInternalLimit": reached_internal_limit,
        "items": spots,
    }


def _normalize_spot_item(keyword: str, item: Dict) -> Optional[Dict]:
    sight_id = item.get("id")
    if sight_id is None:
        return None

    district_id = _extract_district_id(item.get("url", ""))
    if district_id is None:
        return None

    return {
        "keyword": keyword,
        "sight_id": int(sight_id),
        "district_id": int(district_id),
        "code": item.get("code", ""),
        "name": item.get("word", ""),
        "url": item.get("url", ""),
        "image_url": item.get("imageUrl", ""),
        "district_name": item.get("districtName", ""),
        "e_name": item.get("eName", ""),
        "comment_score": item.get("commentScore"),
        "comment_count": item.get("commentCount"),
    }


def _extract_district_id(url: str) -> Optional[int]:
    match = re.search(r"/sight/(\d+)/(\d+)\.html", url)
    if not match:
        return None
    return int(match.group(1))


def _sync_spots_to_mysql(spots: List[Dict]) -> Dict:
    """打印全量数据并执行 MySQL upsert。"""
    logger.info("开始打印全量景点数据，共 %d 条", len(spots))
    for idx, row in enumerate(spots, start=1):
        logger.info("[spot-%d] %s", idx, row)

    if not spots:
        return {
            "table": config.MYSQL_TABLE_SPIDER_SPOTS,
            "wouldUpsert": 0,
            "inserted": 0,
            "updated": 0,
            "note": "无可同步数据。",
        }

    try:
        import pymysql
    except Exception as e:
        raise RuntimeError(f"pymysql 未安装，请先安装依赖: {e}")

    table = config.MYSQL_TABLE_SPIDER_SPOTS
    conn = pymysql.connect(
        host=config.MYSQL_HOST,
        port=config.MYSQL_PORT,
        user=config.MYSQL_USER,
        password=config.MYSQL_PASSWORD,
        database=config.MYSQL_DATABASE,
        charset="utf8mb4",
        autocommit=False,
    )

    existing_key_set = set()
    chunk_size = 200

    try:
        with conn.cursor() as cursor:
            # 先查已有键，用于返回 inserted/updated 统计。
            for i in range(0, len(spots), chunk_size):
                chunk = spots[i:i + chunk_size]
                where_clause = " OR ".join(["(sight_id=%s AND district_id=%s)"] * len(chunk))
                params: List = []
                for row in chunk:
                    params.extend([row["sight_id"], row["district_id"]])

                query = (
                    f"SELECT sight_id, district_id FROM {table} "
                    f"WHERE {where_clause}"
                )
                cursor.execute(query, params)
                rows = cursor.fetchall()
                for sight_id, district_id in rows:
                    existing_key_set.add((int(sight_id), int(district_id)))

            upsert_sql = (
                f"INSERT INTO {table} ("
                "sight_id, district_id, keyword, code, name, url, image_url, "
                "district_name, e_name, comment_score, comment_count"
                ") VALUES ("
                "%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s"
                ") ON DUPLICATE KEY UPDATE "
                "keyword=VALUES(keyword), "
                "code=VALUES(code), "
                "name=VALUES(name), "
                "url=VALUES(url), "
                "image_url=VALUES(image_url), "
                "district_name=VALUES(district_name), "
                "e_name=VALUES(e_name), "
                "comment_score=VALUES(comment_score), "
                "comment_count=VALUES(comment_count), "
                "updated_at=CURRENT_TIMESTAMP"
            )

            values = [
                (
                    row["sight_id"],
                    row["district_id"],
                    row["keyword"],
                    row["code"],
                    row["name"],
                    row["url"],
                    row["image_url"],
                    row["district_name"],
                    row["e_name"],
                    row["comment_score"],
                    row["comment_count"],
                )
                for row in spots
            ]

            cursor.executemany(upsert_sql, values)
        conn.commit()
    except Exception:
        conn.rollback()
        raise
    finally:
        conn.close()

    updated_count = len(existing_key_set)
    inserted_count = max(0, len(spots) - updated_count)

    return {
        "table": table,
        "wouldUpsert": len(spots),
        "inserted": inserted_count,
        "updated": updated_count,
        "note": "已执行 MySQL upsert。",
    }


def _check_mysql_ready() -> Dict:
    """在爬取前做 MySQL 预检查：连接可用且目标表可访问。"""
    try:
        import pymysql
    except Exception as e:
        raise RuntimeError(f"pymysql 未安装，请先安装依赖: {e}")

    table = config.MYSQL_TABLE_SPIDER_SPOTS
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
            cursor.execute("SHOW TABLES LIKE %s", (table,))
            row = cursor.fetchone()
            if not row:
                raise RuntimeError(f"目标表不存在: {table}")
    finally:
        conn.close()

    return {
        "host": config.MYSQL_HOST,
        "port": config.MYSQL_PORT,
        "database": config.MYSQL_DATABASE,
        "table": table,
        "connected": True,
    }
