from collections import Counter
from typing import Any, Dict, Optional

from app.spider.xc_spot_search import xc_fetch_search

DISTRICT_LOOKUP_PAGE_SIZE = 20
DISTRICT_LOOKUP_MAX_PAGES = 3


def resolve_district_context(keyword: str) -> Dict[str, Any]:
    """通过地点名称搜索携程景点，解析并返回最可能的 district_id。"""
    district_counts: Counter[int] = Counter()
    district_names: Dict[int, str] = {}

    for page_index in range(1, DISTRICT_LOOKUP_MAX_PAGES + 1):
        result = xc_fetch_search(keyword, page_index, DISTRICT_LOOKUP_PAGE_SIZE)
        items = result.get("items", []) or []

        for item in items:
            district_id = _extract_district_id(item.get("url", ""))
            if district_id is None:
                continue

            district_counts[district_id] += 1
            district_name = item.get("districtName", "")
            if district_name and district_id not in district_names:
                district_names[district_id] = district_name

        if district_counts:
            break

    if not district_counts:
        raise RuntimeError(f"无法从搜索结果中解析 district_id: {keyword}")

    district_id, matched_count = district_counts.most_common(1)[0]
    return {
        "keyword": keyword,
        "district_id": district_id,
        "district_name": district_names.get(district_id, ""),
        "matched_count": matched_count,
        "candidate_counts": dict(district_counts),
    }


def _extract_district_id(url: str) -> Optional[int]:
    import re

    match = re.search(r"/sight/(\d+)/(\d+)\.html", url or "")
    if not match:
        return None
    return int(match.group(1))