import json
import re
import argparse
from typing import Any, Dict, List

import requests
from bs4 import BeautifulSoup

def _get_headers() -> Dict[str, str]:
    return {
        "accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,"
                  "*/*;q=0.8,application/signed-exchange;v=b3;q=0.7",
        "accept-language": "zh-CN,zh;q=0.9",
        "referer": "https://you.ctrip.com/globalsearch/?keyword=%E9%87%8D%E5%BA%86",
        "sec-ch-ua": "\"Not:A-Brand\";v=\"99\", \"Google Chrome\";v=\"145\", \"Chromium\";v=\"145\"",
        "sec-fetch-site": "same-origin",
        "upgrade-insecure-requests": "1",
        "user-agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) "
                      "Chrome/145.0.0.0 Safari/537.36",
    }


def fetch_html(district_code: int = 158, sight_code: int = 45771) -> str:
    url = f"https://you.ctrip.com/sight/{district_code}/{sight_code}.html"
    response = requests.get(url, headers=_get_headers(), timeout=15)
    response.raise_for_status()
    return response.text


def _clean_text(text: str) -> str:
    return re.sub(r"\s+", " ", text or "").strip()


def _extract_style_url(style_value: str) -> str:
    match = re.search(r"url\((.*?)\)", style_value or "")
    return match.group(1) if match else ""


def _extract_by_module_titles(soup: BeautifulSoup) -> Dict[str, Any]:
    data: Dict[str, Any] = {}
    module = soup.find("div", class_="detailModule normalModule")
    if not module:
        return data

    titles = module.find_all("div", class_="moduleTitle")
    contents = module.find_all("div", class_="moduleContent")
    for title_tag, content_tag in zip(titles, contents):
        title = _clean_text(title_tag.get_text())
        if not title:
            continue
        rows = content_tag.find_all("div", class_="moduleContentRow")
        phones = content_tag.find_all("div", class_="phoneItem")
        if phones:
            phone_items: List[Dict[str, str]] = []
            for item in phones:
                phone_items.append(
                    {
                        "type": _clean_text(item.find("span", class_="phoneItemFunc").get_text())
                        if item.find("span", class_="phoneItemFunc") else "",
                        "number": _clean_text(item.find("span", class_="phoneItemNum").get_text())
                        if item.find("span", class_="phoneItemNum") else "",
                    }
                )
            data[title] = phone_items
        elif rows:
            data[title] = [_clean_text(row.get_text()) for row in rows if _clean_text(row.get_text())]
        else:
            paragraphs = content_tag.find_all("p")
            if paragraphs:
                data[title] = [_clean_text(p.get_text()) for p in paragraphs if _clean_text(p.get_text())]
            else:
                data[title] = _clean_text(content_tag.get_text())
    return data


def parse_spot_detail(html: str) -> Dict[str, Any]:
    soup = BeautifulSoup(html, "html.parser")

    images = [
        _extract_style_url(item.get("style", ""))
        for item in soup.find_all("div", class_="swiperItem")
        if _extract_style_url(item.get("style", ""))
    ]
    thumbnails = [
        _extract_style_url(item.get("style", ""))
        for item in soup.find_all("div", class_="swiperThumbnailItem")
        if _extract_style_url(item.get("style", ""))
    ]
    total_image_text = _clean_text(soup.select_one(".totalCont").get_text()) if soup.select_one(".totalCont") else ""
    total_image_match = re.search(r"(\d+)", total_image_text)
    total_image_count = int(total_image_match.group(1)) if total_image_match else len(images)

    name_tag = soup.find("h1")
    heat_score_tag = soup.select_one(".style_score_text_Big__WN77L")
    review_score_tag = soup.select_one(".XReviewScoreV2_scoreText__l7KJA")
    review_count_tag = soup.select_one(".XReviewScoreV2_countText__uumpj")
    rank_text_tag = soup.find("p", class_="rankText")

    base_info: Dict[str, str] = {}
    for item in soup.select(".baseInfoContent .baseInfoItem"):
        key = item.find("p", class_="baseInfoTitle")
        value = item.find("p", class_="baseInfoText")
        if key and value:
            base_info[_clean_text(key.get_text())] = _clean_text(value.get_text())

    ask_items: List[Dict[str, str]] = []
    for ask in soup.select(".askList .askItem"):
        title = ask.find("span", class_="askTitle")
        answer = ask.find("span", class_="answer")
        ask_items.append(
            {
                "question": _clean_text(title.get_text()) if title else "",
                "answer_count": _clean_text(answer.get_text()) if answer else "",
            }
        )

    comments: List[Dict[str, Any]] = []
    for c in soup.select(".commentList .commentItem"):
        user = c.find("div", class_="userName")
        score = c.select_one(".scroreInfo .averageScore")
        detail = c.find("div", class_="commentDetail")
        time_tag = c.find("div", class_="commentTime")
        ip_tag = c.find("span", class_="ipContent")
        image_urls = [img.get("src", "") for img in c.select(".commentImgList img") if img.get("src")]
        comments.append(
            {
                "user": _clean_text(user.get_text()) if user else "",
                "score": _clean_text(score.get_text()) if score else "",
                "content": _clean_text(detail.get_text()) if detail else "",
                "time": _clean_text(time_tag.get_text()) if time_tag else "",
                "ip": _clean_text(ip_tag.get_text()) if ip_tag else "",
                "images": image_urls,
            }
        )

    module_data = _extract_by_module_titles(soup)
    title_tag = soup.find("title")
    description_tag = soup.find("meta", attrs={"name": "description"})
    keywords_tag = soup.find("meta", attrs={"name": "keywords"})

    return {
        "spot": {
            "name": _clean_text(name_tag.get_text()) if name_tag else "",
            "page_title": _clean_text(title_tag.get_text()) if title_tag else "",
            "description": description_tag.get("content", "") if description_tag else "",
            "keywords": keywords_tag.get("content", "") if keywords_tag else "",
            "heat_score": _clean_text(heat_score_tag.get_text()) if heat_score_tag else "",
            "review_score": _clean_text(review_score_tag.get_text()) if review_score_tag else "",
            "review_count": _clean_text(review_count_tag.get_text()) if review_count_tag else "",
            "rank": _clean_text(rank_text_tag.get_text()) if rank_text_tag else "",
        },
        "base_info": base_info,
        "images": {
            "origin": images,
            "thumbnail": thumbnails,
            "declared_total": total_image_count,
            "fetched_preview_count": len(images),
        },
        "detail_modules": module_data,
        "qa": ask_items,
        "comment_summary": {
            "total": _clean_text(soup.select_one("#commentModule .moduleTitle").get_text())
            if soup.select_one("#commentModule .moduleTitle") else "",
            "average": _clean_text(soup.select_one("#commentModule .averageScore").get_text())
            if soup.select_one("#commentModule .averageScore") else "",
            "hot_tags": [
                _clean_text(tag.get_text())
                for tag in soup.select("#commentModule .hotTags .hotTag")
                if _clean_text(tag.get_text())
            ],
        },
        "comments": comments,
    }


def xc_fetch_detail(district_code: int = 158, sight_code: int = 45771) -> Dict[str, Any]:
    """Fetch and parse a Ctrip scenic spot detail page for API usage."""
    html = fetch_html(district_code, sight_code)
    return parse_spot_detail(html)


def main(district_code: int = 158, sight_code: int = 45771) -> Dict[str, Any]:
    return xc_fetch_detail(district_code, sight_code)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description="Parse Ctrip scenic spot detail page HTML.")
    parser.add_argument("--input", default="response.txt", help="Local HTML file path. If missing, fetch online.")
    parser.add_argument("--output", default="data.json", help="Output JSON file path.")
    parser.add_argument("--district", type=int, default=158, help="District code when fetching online.")
    parser.add_argument("--sight", type=int, default=45771, help="Sight code when fetching online.")
    args = parser.parse_args()

    try:
        with open(args.input, encoding="utf-8", mode="r") as f:
            html_content = f.read()
    except FileNotFoundError:
        html_content = fetch_html(args.district, args.sight)

    result = parse_spot_detail(html_content)
    with open(args.output, encoding="utf-8", mode="w") as f:
        json.dump(result, f, ensure_ascii=False, indent=2)
    print(json.dumps(result, ensure_ascii=False, indent=2))
