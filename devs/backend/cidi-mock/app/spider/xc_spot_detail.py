import requests
from bs4 import BeautifulSoup


def get_origin_images(soup):
    """获取完整图列表"""
    origin_images = []
    swiper_few_complete_soup = soup.find_all("div", class_="swiperItem")
    for item in swiper_few_complete_soup:
        style_str = item.get('style', '')
        if 'url(' in style_str:
            _url = style_str.split('url(')[1].split(')')[0]
            origin_images.append(_url)
    return origin_images


def get_small_images(soup):
    """获取缩略图列表"""
    small_images = []
    swiper_few_small_soup = soup.find_all("div", class_="swiperThumbnailItem")
    for item in swiper_few_small_soup:
        style_str = item.get('style', '')
        if 'url(' in style_str:
            _url = style_str.split('url(')[1].split(')')[0]
            small_images.append(_url)
    return small_images


def get_total_cont(soup):
    """获取图片总数"""
    swiper_total_soup = soup.find("div", class_="totalCont")
    if swiper_total_soup:
        return swiper_total_soup.text[1:][:-1]
    return ''


def get_heat_score(soup):
    """获取景点热度"""
    heat_score_soup = soup.find("div", class_="heatScoreText")
    return heat_score_soup.text if heat_score_soup else ''


def get_spot_intros(normal_info_items):
    """获取景点介绍"""
    if len(normal_info_items) > 0:
        spot_intro_soup = normal_info_items[0].div.div.find_all("p")
        return [str(p) for p in spot_intro_soup]
    return []


def get_open_time(normal_info_items):
    """获取开放时间"""
    if len(normal_info_items) > 1:
        return normal_info_items[1].text.strip()
    return ''


def get_price_policies(normal_info_items):
    """获取优待政策"""
    if len(normal_info_items) > 2:
        price_polices_soup = normal_info_items[2].find_all("div")
        return [item.text.strip() for item in price_polices_soup]
    return []


def get_official_phone(normal_info_items):
    """获取官方电话"""
    if len(normal_info_items) > 3:
        phone_soup = normal_info_items[3].div.find("span", class_="phoneItemNum")
        if phone_soup:
            return phone_soup.text.strip()
    return ''


def get_service_facilities(normal_info_items):
    """获取服务设施"""
    if len(normal_info_items) > 4:
        service_facilities_soup = normal_info_items[4].find_all("div", class_="moduleContentRow")
        return [item.text.strip() for item in service_facilities_soup]
    return []


def xc_fetch_detail(district_code=158, sight_code=45771):
    """查询指定景点的详细信息，参数 code 通过搜索景点获取"""
    # TODO: 获取到所有完整的信息，有一部分在搜索景点的时候就有了，所以没有处理
    # TODO: 优化函数传参的形参，优化内存占用，即先获取到小的 soup ,再传参，而不是直接传根 soup
    # TODO: 没有采集评论区的数据。暂时不考虑做评论区
    headers = {
        "accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,"
                  "*/*;q=0.8,application/signed-exchange;v=b3;q=0.7",
        "accept-language": "zh-CN,zh;q=0.9",
        "referer": "https://you.ctrip.com/globalsearch/?keyword=%E9%87%8D%E5%BA%86",
        "sec-ch-ua": "\"Not:A-Brand\";v=\"99\", \"Google Chrome\";v=\"145\", \"Chromium\";v=\"145\"",
        "sec-fetch-site": "same-origin",
        "upgrade-insecure-requests": "1",
        "user-agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) "
                      "Chrome/145.0.0.0 Safari/537.36"
    }
    url = f"https://you.ctrip.com/sight/{district_code}/{sight_code}.html"
    response_html = requests.get(url, headers=headers).text
    soup = BeautifulSoup(response_html, "html.parser")

    # 解析一般信息板块容器
    normal_info_wrapper_soup = soup.find("div", class_="detailModuleRef")
    normal_module_soup = normal_info_wrapper_soup.find("div", class_="detailModule normalModule")
    normal_info_items = normal_module_soup.find_all("div", class_="moduleContent")

    # 组装返回结果
    ret = {
        'preview_images': {
            'origin_images': get_origin_images(soup),
            'small_images': get_small_images(soup),
            'totalCont': get_total_cont(soup)
        },
        'heatScore': get_heat_score(soup),
        'normal_infos': {
            'intros': get_spot_intros(normal_info_items),
            'open_time': get_open_time(normal_info_items),
            'price_polices': get_price_policies(normal_info_items),
            'official_phone': get_official_phone(normal_info_items),
            'service_facilities': get_service_facilities(normal_info_items)
        }
    }

    return ret


if __name__ == '__main__':
    print(xc_fetch_detail(158, 45771))
