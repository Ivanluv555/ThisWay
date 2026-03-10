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
from pprint import pprint


class SpiderDataKeys:

    def __init__(self):
        self.url = "https://lyfw.mct.gov.cn/site/special/home"
        self.headers = {
            "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7",
            "Accept-Language": "zh-CN,zh;q=0.9",
            "Cache-Control": "max-age=0",
            "Connection": "keep-alive",
            "If-None-Match": "W/\"1ce32d-oSwkJKTCsa/VRA4OoUVd7ewoPyc\"",
            "Referer": "https://lyfw.mct.gov.cn/",
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
        self.soup = None  # BeautifulSoup对象
        self.data_keys = None  # 存储提取的data-key
        self.province_names = None  # 存储提取的省份名称
        self.json_data = None  # 存储最终的JSON数据

    def fetch_html(self):
        """获取网页HTML并返回BeautifulSoup对象"""
        try:
            response = requests.get(self.url, headers=self.headers)
            response.raise_for_status()  # 抛出HTTP请求异常
            self.soup = BeautifulSoup(response.text, "html.parser")
        except requests.exceptions.RequestException as e:
            print(f"获取HTML失败：{e}")
            return None

    def get_data_keys(self):
        """获取每个行政区的 static-key"""
        provinces_group = self.soup.find("g", class_="provinces")
        path_nodes = provinces_group.find_all("path")
        data_keys = [path.get("static-key") for path in path_nodes]
        # 移除最后4个重复的 static-key
        data_keys = data_keys[:-4]
        self.data_keys = data_keys

    def get_province_names(self):
        """获取每个行政区对应的 中文名"""
        texts_group = self.soup.find("g", class_="texts")
        text_elements = texts_group.find_all("text")

        province_names = []
        for text_element in text_elements:
            tspan_node = text_element.find("tspan")
            # 跳过异常元素
            if tspan_node is None:
                continue
            # 清洗文本内容
            text_content = tspan_node.text.strip()
            # 过滤以"5A"开头的内容，只保留行政区名称
            if text_content[:2] == "5A":
                continue
            province_names.append(text_content)

        self.province_names = province_names

    def make_json(self, save_path="province_data.json"):
        """以 json 格式保存data-key和省份名称，默认保存到当前目录的province_data.json"""

        # 组合数据（确保两个列表长度一致）
        if len(self.data_keys) != len(self.province_names):
            print(
                f"数据长度不匹配：data_keys={len(self.data_keys)}, province_names={len(self.province_names)}，源网站可能已更新，请修改代码")
            return

        # 构建字典：省份名称为键，static-key为值
        self.json_data = [
            {"static-key": data_key, "province-text": province_name}
            for data_key, province_name in zip(self.data_keys, self.province_names)
        ]

        try:
            with open(save_path, "w", encoding="utf-8") as f:
                json.dump(self.json_data, f, ensure_ascii=False, indent=4)
            print(f"JSON文件已保存到：{save_path}")
        except IOError as e:
            print(f"保存JSON失败：{e}")

    def print_data(self):
        """打印数据"""
        key_len = len(self.data_keys)
        name_len = len(self.province_names)
        if key_len != name_len:
            print(f"数据长度不匹配：data_keys={key_len}, province_names={name_len}")
            return
        pprint(self.json_data)
        print(f"共有 {len(self.json_data)}/34 个数据，")

    def run(self):
        self.fetch_html()
        self.get_data_keys()
        self.get_province_names()
        self.make_json()


if __name__ == "__main__":
    spider = SpiderDataKeys()
    spider.run()
    spider.print_data()

    """控制台输出示例：
    JSON文件已保存到：province_data.json
    [{'static-key': 'anhui', 'province-text': '安徽'},
     {'static-key': 'neimenggu', 'province-text': '内蒙古'},
    ...
     {'static-key': 'xianggang', 'province-text': '香港'},
     {'static-key': 'aomen', 'province-text': '澳门'}]
    共有 34/34 个数据，
    """
