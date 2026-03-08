from flask import Blueprint, current_app


travel_spot_detail_bp = Blueprint('travel_spot_detail', __name__)

@travel_spot_detail_bp.route('/spot/detail')
def search():
    server_cfg = current_app.config.get("server")

    return "景点数据：测试环境配置获取" + server_cfg.get("host") + ":" + str(server_cfg.get("port"))



