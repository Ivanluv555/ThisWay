try:
    from .xc_spot_search import xc_fetch_search
    from .xc_spot_detail import xc_fetch_detail
except ImportError:
    from xc_spot_search import xc_fetch_search
    from xc_spot_detail import xc_fetch_detail


class XCSpider:

    def __init__(self):
        pass

    def fetch_search(self, keyword="重庆", pageIndex=1, pageSize=12):
        """搜索景点，返回搜索结果"""
        return xc_fetch_search(keyword, pageIndex, pageSize)

    def fetch_detail(self, district_code=158, sight_code=45771):
        """查询指定景点的详细信息，参数 code 通过搜索景点获取"""
        return xc_fetch_detail(district_code, sight_code)


if __name__ == '__main__':
    spider = XCSpider()
    print(spider.fetch_search("重庆", 1, 12))
    print("\n" + "="*50 + "\n")
    print(spider.fetch_detail(158, 45771))
