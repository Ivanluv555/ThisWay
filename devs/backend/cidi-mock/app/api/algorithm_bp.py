from flask import Blueprint, request, jsonify

from app.algorithm.kualk.crowd_index import *

algorithm_bp = Blueprint('algorithm', __name__)


@algorithm_bp.route('/algorithm/crowd_index', methods=['POST'])
def crowd_index():
    data = request.get_json()
    _result = CrowdIndexCalculator.calculate(
        spot_type=data['spot_type'],  # 景区类型：natural, cultural, comprehensive
        infra_type=data['infra_type'],  # 基础设施类型：leisure, experience, comprehensive
        activity_type=data['activity_type'],  # 活动类型：dynamic, static, linear
        N=data['N'],  # 实时游客人数 (整数)
        S_e=data['S_e'],  # 项目有效面积 (平方米，浮点数)
        M=data['M'],  # 工作人员数 (整数)
        F_p=data['F_p'],  # 公共设施总服务能力 (整数)
        F_t=data['F_t'],  # 旅游设施总服务能力 (整数)
        alpha=data['alpha'],  # 空间分布参数 (1.0~1.5，默认 1.0)
    )

    return jsonify({
        'code': 200,
        'message': 'success',
        'data': _result
    })


'''计算景区的压力指数：响应示例
{
  "code": 200,
  "data": {
    "ci": 7.38, // 最终的压力指数 [0,10]
    "i_index": 8.19,
    "level": "轻度超载",
    "s_index": 10.0,
    "v_index": 4.8
  },
  "message": "success"
}
'''
