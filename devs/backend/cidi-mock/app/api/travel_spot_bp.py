from flask import Blueprint, request, jsonify
from app.spider.xc_spider import XCSpider

travel_spot_bp = Blueprint('travel_spot', __name__)


@travel_spot_bp.route('/spot/detail', methods=['POST'])
def detail():
    # 用 JSON 传递数据
    data = request.get_json()
    district_code = data.get('district_code', 158, type=int)
    sight_code = data.get('sight_code', 45771, type=int)

    spider = XCSpider()
    result = spider.fetch_detail(district_code, sight_code)

    return jsonify(result)


@travel_spot_bp.route('/search', methods=['POST'])
def search():
    # 用 JSON 传递数据
    data = request.get_json()
    keyword = data.get('keyword', '重庆')
    page_index = data.get('pageIndex', 1, type=int)
    page_size = data.get('pageSize', 12, type=int)

    spider = XCSpider()
    result = spider.fetch_search(keyword, page_index, page_size)

    return jsonify(result)
