from flask import Blueprint
from .travel_spot_search_bp import travel_spot_search_bp
from .travel_spot_detail_bp import travel_spot_detail_bp

# 创建总蓝图
api_bp = Blueprint("api", __name__, url_prefix="/api/v1")

# 注册子模块
api_bp.register_blueprint(travel_spot_search_bp)
api_bp.register_blueprint(travel_spot_detail_bp)
