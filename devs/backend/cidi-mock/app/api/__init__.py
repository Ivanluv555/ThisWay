from flask import Blueprint
from .travel_spot_bp import travel_spot_bp
from .api_doc_bp import api_doc_bp
from .algorithm_bp import algorithm_bp

api_bp = Blueprint("api", __name__, url_prefix="/api/v1")

api_bp.register_blueprint(travel_spot_bp)
api_bp.register_blueprint(api_doc_bp)
api_bp.register_blueprint(algorithm_bp)
