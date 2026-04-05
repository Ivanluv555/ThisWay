"""
cidi-model 烟雾测试
测试 Spring Boot 接口 ↔ 数据库 通路是否全部通畅
不依赖 torch，可在任何 Python 环境下运行。
"""
import sys
from datetime import datetime, timedelta

import httpx

SPRING = "http://localhost:8080"
OK = "\033[92m✓\033[0m"
FAIL = "\033[91m✗\033[0m"
errors = []


def check(label: str, ok: bool, detail: str = ""):
    sym = OK if ok else FAIL
    print(f"  {sym}  {label}{': ' + detail if detail else ''}")
    if not ok:
        errors.append(label)


def section(title: str):
    print(f"\n{'='*50}")
    print(f"  {title}")
    print("=" * 50)


# ── 1. Spring Boot 健康检查 ────────────────────────────────────────────────────
section("1. Spring Boot 连通性")
try:
    r = httpx.get(f"{SPRING}/api/locations", timeout=5)
    check("GET /api/locations 返回 200", r.status_code == 200)
    locations = r.json()
    check("返回景点数量 == 21", len(locations) == 21, f"实际={len(locations)}")
    loc_ids = [l["id"] for l in locations]
    check("景点包含 ID 1 和 21", 1 in loc_ids and 21 in loc_ids)
except Exception as e:
    check("GET /api/locations", False, str(e))

# ── 2. location_hourly_flow 写入 ──────────────────────────────────────────────
section("2. location_hourly_flow 写入（POST /api/flow/history/batch）")
test_hour = (datetime.now() - timedelta(hours=5)).replace(minute=0, second=0, microsecond=0)
test_hour_str = test_hour.strftime("%Y-%m-%dT%H:%M:%S")
batch = [
    {"locationId": loc_id, "visitorCount": 100 + loc_id * 10,
     "recordHour": test_hour_str, "dataSource": "smoke_test"}
    for loc_id in range(1, 6)          # 写 5 条
]
try:
    r = httpx.post(f"{SPRING}/api/flow/history/batch", json=batch, timeout=8)
    check("POST /api/flow/history/batch 返回 200", r.status_code == 200)
    saved = r.json()
    check("返回条数 == 5", len(saved) == 5, f"实际={len(saved)}")
    check("写入 ID 非空", all(x.get("id") for x in saved))
except Exception as e:
    check("POST /api/flow/history/batch", False, str(e))

# ── 3. location_hourly_flow 查询 ──────────────────────────────────────────────
section("3. location_hourly_flow 查询（GET /api/flow/history）")
try:
    r = httpx.get(f"{SPRING}/api/flow/history?hours=6", timeout=5)
    check("GET /api/flow/history?hours=6 返回 200", r.status_code == 200)
    rows = r.json()
    check("回读到刚写入的 5 条数据", len(rows) >= 5, f"实际={len(rows)}")
    # 按 locationId 过滤查询
    r2 = httpx.get(f"{SPRING}/api/flow/history?hours=6&locationId=1", timeout=5)
    check("按 locationId=1 过滤查询 200", r2.status_code == 200)
    rows2 = r2.json()
    check("过滤结果全是 locationId=1", all(x["locationId"] == 1 for x in rows2))
except Exception as e:
    check("GET /api/flow/history", False, str(e))

# ── 4. spot_traffic_predictions 写入 ─────────────────────────────────────────
section("4. spot_traffic_predictions 写入（POST /api/spot-traffic-predictions/batch）")
now_str = datetime.now().replace(minute=0, second=0, microsecond=0).strftime("%Y-%m-%dT%H:%M:%S")
pred_batch = [
    {
        "spotId": loc_id,
        "predictTime": now_str,
        "visitorCount": 500 + loc_id * 20,
        "congestionLevel": 2,
        "trend": "平稳",
        "trafficLevel": 2,
        "crowdDensity": 0.45,
    }
    for loc_id in range(1, 4)          # 写 3 条预测
]
try:
    r = httpx.post(f"{SPRING}/api/spot-traffic-predictions/batch",
                   json=pred_batch, timeout=8)
    check("POST /api/spot-traffic-predictions/batch 返回 200", r.status_code == 200)
    saved = r.json()
    check("返回条数 == 3", len(saved) == 3, f"实际={len(saved)}")
except Exception as e:
    check("POST /api/spot-traffic-predictions/batch", False, str(e))

# ── 5. spot_traffic_predictions 最新一轮查询 ──────────────────────────────────
section("5. 预测最新一轮查询（GET /api/spot-traffic-predictions/latest）")
try:
    r = httpx.get(f"{SPRING}/api/spot-traffic-predictions/latest", timeout=5)
    check("GET /api/spot-traffic-predictions/latest 返回 200", r.status_code == 200)
    latest = r.json()
    check("返回至少 3 条最新预测", len(latest) >= 3, f"实际={len(latest)}")
    if latest:
        check("首条含 visitorCount 字段", "visitorCount" in latest[0])
except Exception as e:
    check("GET /api/spot-traffic-predictions/latest", False, str(e))

# ── 汇总 ──────────────────────────────────────────────────────────────────────
section("测试汇总")
total = 15   # 预期检查项总数
if errors:
    print(f"  {FAIL}  {len(errors)} 项失败: {errors}")
    sys.exit(1)
else:
    print(f"  {OK}  全部检查通过，通路 OK！")
    sys.exit(0)
