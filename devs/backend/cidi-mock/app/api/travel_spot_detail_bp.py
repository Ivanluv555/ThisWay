from flask import Blueprint, request, jsonify
from app.spider.xc_spider import XCSpider

travel_spot_detail_bp = Blueprint('travel_spot_detail', __name__)

# TODO: 改为 post 请求，以及传参方式
@travel_spot_detail_bp.route('/spot/detail')
def detail():
    district_code = request.args.get('district_code', 158, type=int)
    sight_code = request.args.get('sight_code', 45771, type=int)
    
    spider = XCSpider()
    result = spider.fetch_detail(district_code, sight_code)
    
    return jsonify(result)
