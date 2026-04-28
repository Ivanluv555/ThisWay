from __future__ import annotations

from typing import Dict, Iterable, List, Mapping

from CrowdIndex_Calc import ZoneInput, compute_scenic_crowd_index


def _build_template_zones(visitor_count: int) -> List[ZoneInput]:
    """按测试模板将单景区客流拆分为多个功能区。"""
    v = max(0, int(visitor_count))

    entry_people = int(v * 0.18)
    ride_people = int(v * 0.15)
    toilet_people = int(v * 0.08)

    entry_wait = max(0.0, min(60.0, (entry_people - 650) / 25.0))
    ride_wait = max(0.0, min(60.0, (ride_people - 380) / 18.0))
    toilet_wait = max(0.0, min(45.0, (toilet_people - 120) / 12.0))

    return [
        ZoneInput(
            zone_id="ENTRY_MAIN",
            zone_type="ENTRY",
            max_capacity=1200,
            people_now=entry_people,
            wait_minutes=entry_wait,
        ),
        ZoneInput(
            zone_id="CORE_VIEW",
            zone_type="CORE",
            max_capacity=2600,
            people_now=int(v * 0.42),
        ),
        ZoneInput(
            zone_id="RIDE_A",
            zone_type="RIDE",
            max_capacity=900,
            people_now=ride_people,
            wait_minutes=ride_wait,
        ),
        ZoneInput(
            zone_id="TOILET_A",
            zone_type="TOILET",
            max_capacity=260,
            people_now=toilet_people,
            wait_minutes=toilet_wait,
        ),
        ZoneInput(
            zone_id="PATH_MAIN",
            zone_type="PATH",
            max_capacity=4200,
            people_now=int(v * 0.17),
        ),
    ]


def compute_location_congestion_index(visitor_count: int) -> float:
    """计算单景区拥挤指数，返回 0~10。"""
    scenic_index = compute_scenic_crowd_index(
        zones=_build_template_zones(visitor_count),
        weather_level="sunny",
        manage_state="normal",
        strict=False,
    )
    return round(max(0.0, min(10.0, scenic_index * 10.0)), 2)


def compute_batch_congestion_index(visitor_map: Mapping[int, int]) -> Dict[int, float]:
    """批量计算景区拥挤指数，返回 {location_id: congestion_index(0~10)}。"""
    return {
        int(location_id): compute_location_congestion_index(int(visitor_count))
        for location_id, visitor_count in visitor_map.items()
    }


def build_crowd_index_items(rows: Iterable[Mapping[str, object]]) -> List[Dict[str, object]]:
    """把带 visitor_count 的地点列表转换成 crowd-index 响应项。"""
    items: List[Dict[str, object]] = []
    for row in rows:
        location_id = int(row["location_id"])
        visitor_count = int(row["visitor_count"])
        congestion_index = compute_location_congestion_index(visitor_count)
        items.append(
            {
                "locationId": location_id,
                "locationTitle": str(row.get("title", "")),
                "visitorCount": visitor_count,
                "congestionIndex": congestion_index,
            }
        )
    return items
