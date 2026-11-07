# 用处不大，暂不考虑使用
# 用处不大，暂不考虑使用
# 用处不大，暂不考虑使用
# 用处不大，暂不考虑使用
# 用处不大，暂不考虑使用
# 用处不大，暂不考虑使用
# 用处不大，暂不考虑使用
# 用处不大，暂不考虑使用

import requests
import json
from bs4 import BeautifulSoup

headers = {
    "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7",
    "Accept-Language": "zh-CN,zh;q=0.9",
    "Cache-Control": "max-age=0",
    "Connection": "keep-alive",
    "If-None-Match": "W/\"14212-TFYKap2z88ZKJwA3xY8km0britU\"",
    "Sec-Fetch-Dest": "document",
    "Sec-Fetch-Mode": "navigate",
    "Sec-Fetch-Site": "same-origin",
    "Sec-Fetch-User": "?1",
    "Upgrade-Insecure-Requests": "1",
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/145.0.0.0 Safari/537.36",
    "sec-ch-ua": "\"Not:A-Brand\";v=\"99\", \"Google Chrome\";v=\"145\", \"Chromium\";v=\"145\"",
    "sec-ch-ua-mobile": "?0",
    "sec-ch-ua-platform": "\"Windows\""
}
url = "https://lyfw.mct.gov.cn/site/special/province"

# provinces = ["chongqing", "sichuan"]
provinces = []
with open("province_data.json", mode="r", encoding="utf-8") as f:
    provinces = json.load(f)


# 存储所有景区数据
all_scenic_data = []

for province in provinces:
    print(f"正在爬取 {province['province-text']} 省份的景区数据...")
    params = {"pkey": province['static-key'], "type": "0"}
    response = requests.get(url, headers=headers, params=params)
    soup = BeautifulSoup(response.text, "html.parser")

    spot_divs = soup.find_all("div", class_="spot")

    for spot in spot_divs:
        scenic_data = {}
        try:
            # 景区名称
            name_elem = spot.find("div", class_="t")
            scenic_data["景区名称"] = name_elem.get_text(strip=True) if name_elem else "未知"
            # 位置
            location_elem = spot.find("div", class_="wz")
            scenic_data["位置"] = location_elem.get_text(strip=True).replace("位置：", "") if location_elem else "未知"
            # 日期/星期/农历
            time_elem = spot.find("div", class_="time1")
            if time_elem:
                time_spans = time_elem.find_all("span")
                scenic_data["日期"] = time_spans[0].get_text(strip=True) if len(time_spans) > 0 else "未知"
                scenic_data["星期"] = time_spans[1].get_text(strip=True) if len(time_spans) > 1 else "未知"
                scenic_data["农历"] = time_spans[3].get_text(strip=True) if len(time_spans) > 3 else "未知"
            else:
                scenic_data["日期"] = scenic_data["星期"] = scenic_data["农历"] = "未知"
            # 天气相关
            weather_elem = spot.find("div", class_="weather")
            if weather_elem:
                # 天气状况（阴/雾等）
                weather_status_elem = weather_elem.find("div", class_="weat_div1").find("div", class_="p1")
                scenic_data["天气状况"] = weather_status_elem.get_text(strip=True) if weather_status_elem else "未知"
                # 温度（当前温度 + 温度范围）
                temp_divs = weather_elem.find_all("div", class_="weat_div")[1:]  # 跳过第一个天气状况div
                if len(temp_divs) >= 1:
                    temp_spans = temp_divs[0].find_all("span")
                    scenic_data["当前温度"] = temp_spans[0].get_text(strip=True) if len(temp_spans) > 0 else "未知"
                    scenic_data["温度范围"] = temp_divs[0].find("div", class_="p1").get_text(strip=True) if temp_divs[
                        0].find("div", class_="p1") else "未知"
                else:
                    scenic_data["当前温度"] = scenic_data["温度范围"] = "未知"
                # AQI（空气质量）
                if len(temp_divs) >= 2:
                    aqi_spans = temp_divs[1].find_all("span")
                    scenic_data["AQI等级&数值"] = aqi_spans[0].get_text(strip=True) if len(aqi_spans) > 0 else "未知"
                    scenic_data["AQI说明"] = temp_divs[1].find("div", class_="p1").get_text(strip=True) if temp_divs[
                        1].find("div", class_="p1") else "未知"
                else:
                    scenic_data["AQI等级&数值"] = scenic_data["AQI说明"] = "未知"
            else:
                scenic_data["天气状况"] = scenic_data["当前温度"] = scenic_data["温度范围"] = scenic_data[
                    "AQI等级&数值"] = scenic_data["AQI说明"] = "未知"
            # 5. 所属省份（便于区分）
            scenic_data["所属省份标识"] = province['province-text']
            # 添加到总列表
            all_scenic_data.append(scenic_data)

        except Exception as e:
            print(f"提取单个景区数据出错：{e}")
            continue
    break

# 要爬取所有景区，就将 break 注释掉
print("\n===== 爬取到的景区数据 =====")
for idx, data in enumerate(all_scenic_data, 1):
    print(f"\n【景区{idx}】")
    for key, value in data.items():
        print(f"{key}：{value}")

# TODO: 网页中的 script 里面写死了关于景区的介绍内容。re 正则获取

"""示例输出：
【景区1】
景区名称：武陵山大裂谷景区
位置：重庆市涪陵区武陵乡
日期：03月07日
星期：周六
农历：农历正月十九
天气状况：雾
当前温度：5.7℃
温度范围：4℃~7℃
AQI等级&数值：优 23
AQI说明：AQI
所属省份标识：chongqing
"""