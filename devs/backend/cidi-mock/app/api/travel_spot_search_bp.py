from flask import Blueprint, request, jsonify
from app.spider.xc_spider import XCSpider

travel_spot_search_bp = Blueprint('travel_spot_search', __name__)

@travel_spot_search_bp.route('/search')
def search():
    keyword = request.args.get('keyword', '重庆')
    page_index = request.args.get('pageIndex', 1, type=int)
    page_size = request.args.get('pageSize', 12, type=int)
    
    spider = XCSpider()
    result = spider.fetch_search(keyword, page_index, page_size)
    
    return jsonify(result)


