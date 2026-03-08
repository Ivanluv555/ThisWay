import requests
import json
from pprint import pprint

headers = {
    "accept": "*/*",
    "accept-language": "zh-CN,zh;q=0.9",
    "content-type": "application/json",
    "cookieorigin": "https://you.ctrip.com",
    "origin": "https://you.ctrip.com",
    "priority": "u=1, i",
    "referer": "https://you.ctrip.com/",
    "sec-ch-ua": "\"Not:A-Brand\";v=\"99\", \"Google Chrome\";v=\"145\", \"Chromium\";v=\"145\"",
    "sec-ch-ua-mobile": "?0",
    "sec-ch-ua-platform": "\"Windows\"",
    "sec-fetch-dest": "empty",
    "sec-fetch-mode": "cors",
    "sec-fetch-site": "same-site",
    "user-agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/145.0.0.0 Safari/537.36"
}
cookies = {
    "ibulanguage": "ZH-CN",
    "ibulocale": "zh_cn",
    "cookiePricesDisplayed": "CNY",
    "UBT_VID": "1772899151510.4702G7XpnCYK",
    "GUID": "09031174315520149362",
    "MKT_OrderClick": "ASID=4899963772&AID=4899&CSID=963772&OUID=&CT=1772899152463&CURL=https%3A%2F%2Fhotels.ctrip.com%2F%3Fallianceid%3D4899%26sid%3D963772%26tyexact6058%26gclsrc%3Daw.ds%26gad_source%3D1%26gad_campaignid%3D2047375382%26gbraid%3D0AAAAACtzBadX838ID7eaGaoRjp9Pde-vr%26gclid%3DCjwKCAiAtq_NBhA_EiwA78nNWElCuyXSOfLckP_BmEFcjXPFnHyhBIBYv6AtAuQfKuXd_3OUb5-B5BoCdUYQAvD_BwE%26keywordid%3D317487730504-70746766525&VAL={\"pc_vid\":\"1772899151510.4702G7XpnCYK\"}",
    "Hm_lvt_a8d6737197d542432f4ff4abc6e06384": "1772899153",
    "Hm_lpvt_a8d6737197d542432f4ff4abc6e06384": "1772899153",
    "HMACCOUNT": "5D72DEACB192F7DF",
    "IBU_showtotalamt": "0",
    "MKT_CKID": "1772899153347.wa6mm.9hvz",
    "Session": "smartlinkcode=U135371&smartlinklanguage=zh&SmartLinkKeyWord=&SmartLinkQuary=&SmartLinkHost=",
    "Union": "AllianceID=4899&SID=135371&OUID=&createtime=1772899153&Expires=1773503953370",
    "_gcl_aw": "GCL.1772899153.CjwKCAiAtq_NBhA_EiwA78nNWElCuyXSOfLckP_BmEFcjXPFnHyhBIBYv6AtAuQfKuXd_3OUb5-B5BoCdUYQAvD_BwE",
    "_gcl_dc": "GCL.1772899153.CjwKCAiAtq_NBhA_EiwA78nNWElCuyXSOfLckP_BmEFcjXPFnHyhBIBYv6AtAuQfKuXd_3OUb5-B5BoCdUYQAvD_BwE",
    "_ga": "GA1.1.342976471.1772899155",
    "_RGUID": "0a568cc6-b4fd-4ae9-8fd9-13c6c12659fd",
    "MKT_Pagesource": "PC",
    "_ga_5DVRDQD429": "GS2.1.s1772899154$o1$g0$t1772899157$j57$l0$h1871514617",
    "_ga_B77BES1Z8Z": "GS2.1.s1772899154$o1$g0$t1772899157$j57$l0$h0",
    "_ga_9BZF483VNQ": "GS2.1.s1772899154$o1$g0$t1772899157$j57$l0$h0",
    "StartCity_Pkg": "PkgStartCity=4",
    "nfes_isSupportWebP": "1",
    "_bfa": "1.1772899151510.4702G7XpnCYK.1.1772899271446.1772899303977.1.7.290510",
    "_jzqco": "%7C%7C%7C%7C1772899154085%7C1.350981943.1772899153359.1772899271935.1772899304201.1772899271935.1772899304201.0.0.0.7.7"
}
url = "https://m.ctrip.com/restapi/soa2/20591/getGsOnlineResult"
params = {
    "_fxpcqlniredt": "09031174315520149362",
    "x-traceID": "09031174315520149362-1772899320457-8271223"
}

for page in range(1, 5):
    data = {
        "keyword": "重庆",
        "pageIndex": page,
        "pageSize": 12,
        "tab": "sight",
        "sourceFrom": "",
        "profile": False,
        "head": {
            "cid": "09031174315520149362",
            "ctok": "",
            "cver": "1.0",
            "lang": "01",
            "sid": "8888",
            "syscode": "09",
            "auth": "",
            "xsid": "",
            "extension": []
        }
    }
    data = json.dumps(data, separators=(',', ':'))
    response = requests.post(url, headers=headers, cookies=cookies, params=params, data=data)
    json_data = response.json()
    total = json_data['total']
    # TODO: 根据第一页获取到的总页数，更新总页数
    pageCount = json_data['pageCount']
    items = json_data['items']
    hasNext = json_data['hasNext']  # bool

    items = items[:3]
    for item in items:
        print(item['code'])
        print(item['commentCount'])  # 15871
        print(item['commentCountText'])  # 999+条评论
        print(item['commentScore'])
        print(item['districtName'])
        print(item['eName'])  # 英文景点名
        print(item['id'])
        print(item['imageUrl'])
        print(item['type'])  # sight 景点
        print(item['url'])
        print(item['word'])  # 武隆天生三桥, 重庆

    print("=" * 50)
    print()
    break

"""
sight_45771
15871
999+条评论
4.6
重庆
Three Natural Bridges
45771
https://dimg04.c-ctrip.com/images/0105q120004gsttsa454D_C_320_320.jpg
sight
https://you.ctrip.com/sight/158/45771.html
武隆天生三桥, 重庆
sight_43811
14407
999+条评论
4.6
重庆
Ciqikou
43811
https://dimg04.c-ctrip.com/images/0106z120008iqyn71AE84_C_320_320.jpg
sight
https://you.ctrip.com/sight/158/43811.html
磁器口古镇, 重庆
sight_109940
16142
999+条评论
4.1
重庆
Yangtze River Cableway
109940
https://dimg04.c-ctrip.com/images/01A511200090idw6fDB4A_C_320_320.jpg
sight
https://you.ctrip.com/sight/158/109940.html
长江索道, 重庆
==================================================
"""