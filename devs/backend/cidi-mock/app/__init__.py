from flask import Flask
from .utils.config import get_config
from .api import api_bp


def create_app():
    app = Flask(__name__)
    app.config.update(get_config())  # 加载配置
    app.register_blueprint(api_bp)  # 注册蓝图(接口)
    return app
