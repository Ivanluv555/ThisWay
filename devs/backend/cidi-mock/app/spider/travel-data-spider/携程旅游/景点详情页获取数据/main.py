import re
import requests
from bs4 import BeautifulSoup
from pprint import pprint


# TODO: 有部分数据与景区查询的数据是重合的，因此没有再重复获取，自己拼接

def get_root_soup():
    headers = {
        "accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7",
        "accept-language": "zh-CN,zh;q=0.9",
        "cache-control": "max-age=0",
        "priority": "u=0, i",
        "referer": "https://you.ctrip.com/globalsearch/?keyword=%E9%87%8D%E5%BA%86",
        "sec-ch-ua": "\"Not:A-Brand\";v=\"99\", \"Google Chrome\";v=\"145\", \"Chromium\";v=\"145\"",
        "sec-ch-ua-mobile": "?0",
        "sec-ch-ua-platform": "\"Windows\"",
        "sec-fetch-dest": "document",
        "sec-fetch-mode": "navigate",
        "sec-fetch-site": "same-origin",
        "sec-fetch-user": "?1",
        "upgrade-insecure-requests": "1",
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
        "Hm_lvt_e4211314613fcf074540918eb10eeecb": "1772899272,1772938468",
        "Hm_lpvt_e4211314613fcf074540918eb10eeecb": "1772938468",
        "HMACCOUNT": "5D72DEACB192F7DF",
        "_bfa": "1.1772899151510.4702G7XpnCYK.1.1772901428107.1772938468271.2.1.290510",
        "_jzqco": "%7C%7C%7C%7C1772899154085%7C1.350981943.1772899153359.1772901428349.1772938469367.1772901428349.1772938469367.0.0.0.13.13",
        "_pd": "%7B%22_o%22%3A81%2C%22s%22%3A938%2C%22_s%22%3A8%7D"
    }
    url = "https://you.ctrip.com/sight/158/45771.html"
    params = {
        "renderPlatform": ""
    }
    response = requests.get(url, headers=headers, cookies=cookies, params=params)
    _soup = BeautifulSoup(response.text, "html.parser")
    return _soup


def get_swiper_image_urls(soup):
    # 该景区的 4 张预览图片-完整图-无压缩
    swiper_few_complete_soup = soup.find_all("div", class_="swiperItem")
    image_urls = []
    for item in swiper_few_complete_soup:
        style_str = item.get('style', '')
        url = style_str.split('url(')[1].split(')')[0]
        print("完整图：" + url)
        image_urls.append(url)
    print("\n" + "=" * 50 + "\n")
    # 该景区的 4 张预览图片-缩略图-不放大的时候用的
    swiper_few_small_soup = soup.find_all("div", class_="swiperThumbnailItem")
    image_urls = []
    for item in swiper_few_small_soup:
        style_str = item.get('style', '')
        url = style_str.split('url(')[1].split(')')[0]
        print("缩略图：" + url)
        image_urls.append(url)

    print("\n" + "=" * 50 + "\n")

    # 景点预览图片总数
    swiper_total_soup = soup.find("div", class_="totalCont")
    match = re.search(r"(\d+)", swiper_total_soup.text)
    swiper_total_int = int(match.group(1))
    print(f"一共有 {swiper_total_int} 张景点预览图片")
    swiper_full_url = swiper_total_soup.a.get("href")
    print(f"所有预览图的网页链接 {swiper_full_url} ")  # TODO: 爬取所有预览图的链接，有需要加载更多再爬，暂时留着


def get_hot_score(soup):
    # 景点热度
    heatScoreText = soup.find("div", class_="heatScoreText").text
    print(f"景点热度：{heatScoreText}")


def normal_info(normal_info_items):
    print("以下数据属于一般信息板块：")

    # 景点介绍
    spot_intro_soup = normal_info_items[0].div.div.find_all("p")
    print("景点介绍：")
    pprint(spot_intro_soup)

    print("\n" + "=" * 50 + "\n")

    # 开放时间
    open_time = normal_info_items[1]
    print(f"开放时间：{open_time.text}")

    print("\n" + "=" * 50 + "\n")

    # 优待政策
    price_polices_soup = normal_info_items[2].find_all("div")
    print(f"优待政策：")
    price_polices = [item.text for item in price_polices_soup]
    pprint(price_polices)

    print("\n" + "=" * 50 + "\n")

    # 官方电话
    official_phone = normal_info_items[3].div.find("span", class_="phoneItemNum").text
    print(f"官方电话：{official_phone}")

    print("\n" + "=" * 50 + "\n")

    # 服务设施
    service_facilities_soup = normal_info_items[4].find_all("div", class_="moduleContentRow")
    print(f"服务设施：")
    pprint([item.text for item in service_facilities_soup])


if __name__ == '__main__':

    root_soup = get_root_soup()
    normal_info_wrapper_soup = root_soup.find("div", class_="detailModuleRef").find("div",
                                                                                    class_="detailModule normalModule")
    _normal_info_items = normal_info_wrapper_soup.find_all("div", class_="moduleContent")

    get_swiper_image_urls(root_soup)
    get_hot_score(root_soup)
    normal_info(_normal_info_items)

    print("通过构造 URL 地址，即可获取到所有景点的详细数据")
    print("URL 通过景区查询脚本获得")
    print("可以弄个数据表，专门来存储对应每个景点的URL，或者缓存查询结果。减少爬网站的次数！！！")


"""
完整图：https://dimg04.c-ctrip.com/images/0105q120004gsttsa454D_W_640_10000.jpg?proc=crop/m_c,w_2500,h_1666,x_0,y_0,A7E3
完整图：https://dimg04.c-ctrip.com/images/0100212000jtjhm24EE03_W_640_10000.jpg?proc=crop/m_c,w_2500,h_1666,x_0,y_1,9804
完整图：https://dimg04.c-ctrip.com/images/0104c12000jtji60h107B_W_640_10000.jpg?proc=crop/m_c,w_2500,h_1666,x_0,y_0,A7E3
完整图：https://dimg04.c-ctrip.com/images/0HJ1q12000rpnyru80FB5_W_640_10000.jpg

==================================================

缩略图：https://dimg04.c-ctrip.com/images/0105q120004gsttsa454D_D_256_180.jpg
缩略图：https://dimg04.c-ctrip.com/images/0100212000jtjhm24EE03_D_256_180.jpg
缩略图：https://dimg04.c-ctrip.com/images/0104c12000jtji60h107B_D_256_180.jpg
缩略图：https://dimg04.c-ctrip.com/images/0HJ1q12000rpnyru80FB5_D_256_180.jpg

==================================================

一共有 29 张景点预览图片
所有预览图的网页链接 https://you.ctrip.com/photos/sight/shanghai2/r45771.html 
景点热度：8.4
以下数据属于一般信息板块：
景点介绍：
[<p>武隆天生三桥，属于典型的喀斯特地貌，罕见的地质奇观旅游区，也是电影《满城尽带黄金甲》和《变形金刚4》的取景地。景区位于武隆县仙女山镇，游客一般是在仙女镇的游客中心购票后，乘坐景区提供的环保车到达景区。</p>,
 <p><strong>三桥夹两坑</strong></p>,
 <p>武隆天生三桥由重庆武隆天生三桥风景区-天龙桥、武隆天生三桥风景区-青龙桥、武隆天生三桥风景区-黑龙桥三座天生石拱桥组成。三座桥横跨在羊水河峡谷之上，石桥的跨度和距离峡谷底部均在百米以上。三座石桥将峡谷两岸的山体连在一起，形成了“三桥夹两坑”的奇特景观。石桥之间所夹的坑，也称为“天坑”。</p>,
 <p><strong>游玩路线</strong></p>,
 <p>游客进入景区后，可以乘坐观光电梯下到谷底（电梯费用包括在门票内）的天坑中，再沿着坑底幽静的小道步行游览。此时环顾四周，满目苍翠，有溪水流淌；抬头仰望，四处是悬崖万丈，有山泉从上飞泻而下。</p>,
 <p><strong>电影场景</strong></p>,
 <p>在天龙桥附近的天坑中，建有仿唐建筑“天福客栈”，就是张艺谋导演的《满城尽带黄金甲》的外景地，这座客栈耗资千万。这里还是《变形金刚4》的中国取景地，如今景区内还有电影中机械怪物的造型。</p>,
 <p><strong>乘坐环保车去龙水峡</strong></p>,
 <p>游玩后，你可以走到景区出口，途中有一段上坡路，也可以乘坐电瓶车到出口。游客在景区出口，可以乘坐环保车回到镇上的游客中心，或者在这里乘坐环保车前往附近的龙水峡地缝继续游玩（需要先购买地缝的门票才能乘车前往）。</p>,
 <p>
<br/>
</p>]

==================================================

开放时间：09/01-04/30 08:30-16:30开放;05/01-05/31 08:30-17:00开放;06/01-08/31 08:00-17:00开放

==================================================

优待政策：
['儿童：6周岁（含）以下凭身份证/户口本或身高1.2米（含）以下，门票免费',
 '未成年人 ：6周岁（不含）-18周岁（含），凭身份证/户口本，门票半价',
 '学生：全日制本科及以下学历学生（需提供本人有效学生证件），门票半价',
 '老人： 60周岁（含）-64周岁（含），凭身份证，门票半价；65周岁（含）以上，凭身份证，门票免费',
 '残疾人：持残疾证（视力、智力残疾、一二级肢体残疾人的一名陪护人员享受同等政策），门票免费',
 '旅行社经理：旅行社经理资格证，门票免费',
 '记者： 国家新闻总署颁发的记者证，门票免费',
 '武隆户籍居民：武隆本地户籍游客（居住地为武隆），门票半价',
 '现役军人：凭军人证，门票免费',
 '退役军人：凭有效证件，门票免费',
 '现役军人家属(军人的配偶、父母(抚养人) 、未成年子女、不能独立生活的成年子女)：凭有效证件，门票免费',
 '"三属"人员：烈士遗属、因公牺牲军人遗属、病故军人遗属凭优待证，门票免费',
 '消防人员：凭有效证件，门票免费',
 '离休干部：凭离退休干部证，免费',
 '导游：凭导游证，免费']

==================================================

官方电话：4000235666

==================================================

服务设施：
['停车场：【天生三桥景区停车场】参考价格：¥20/次；地址：重庆武隆县城东南20公里的白果乡与核桃乡交界处天生三桥景区；库位：100',
 'WIFI：账号：wulonglvyou；密码：通过短信验证码；范围：检票口、驿站（有）',
 '手机充电：游客可在仙女山镇游客接待中心享受手机充电服务',
 '景区讲解：景区提供景区讲解服务，费用：天生三桥中级讲解120元/次；高级200元/次',
 '园内交通：参考价格：15元/人；地址：天生三桥1号门；运行于天生三桥坑顶-坑底',
 '卫生间：天生三桥景区内设有多个卫生间，分别在游客中心等附近并标有醒目的指示牌。',
 '便利店：景区内设有便利店，游客接待中心有便利店']
通过构造 URL 地址，即可获取到所有景点的详细数据
URL 所需参数为：行政区对应的 code，景点对应的 id
景点对应的id可以通过景区查询获取。例如查询结果中的：sight_45771
行政区的code也可以通过景区查询获取。例如查询结果中的：sight_45771

Process finished with exit code 0
"""