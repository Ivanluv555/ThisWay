import math
import json
import requests
from datetime import datetime


def xc_fetch_search(keyword="重庆", pageIndex=1, pageSize=12):
    """搜索景点，返回搜索结果"""
    # TODO: 请求频率检测
    # TODO: 搜索结果缓存
    # TODO: 异常处理
    headers = {
        "accept-language": "zh-CN,zh;q=0.9",
        "content-type": "application/json",
        "origin": "https://you.ctrip.com",
        "referer": "https://you.ctrip.com/",
        "sec-fetch-mode": "cors",
        "user-agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) "
                      "Chrome/145.0.0.0 Safari/537.36"
    }
    url = "https://m.ctrip.com/restapi/soa2/20591/getGsOnlineResult"
    data = {
        "keyword": keyword,
        "pageIndex": pageIndex,
        "pageSize": pageSize,
        "tab": "sight"
    }
    data = json.dumps(data, separators=(',', ':'))

    response = requests.post(url, headers=headers, data=data)

    response = response.json()
    ret = {
        'total': response['total'],
        'pageCount': response['pageCount'],
        'items': response['items'],
        'hasNext': response['hasNext'],
        'timestamp': math.ceil(datetime.now().timestamp())
    }
    return ret


if __name__ == '__main__':
    print(xc_fetch_search("重庆", 1, 12))

