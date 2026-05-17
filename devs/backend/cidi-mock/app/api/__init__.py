from pathlib import Path
from flask import Blueprint
from .travel_spot_search_bp import travel_spot_search_bp
from .travel_spot_detail_bp import travel_spot_detail_bp
from .api_doc_bp import api_doc_bp
from .algorithm_bp import algorithm_bp

api_bp = Blueprint("api", __name__, url_prefix="/api/v1")

api_bp.register_blueprint(travel_spot_search_bp)
api_bp.register_blueprint(travel_spot_detail_bp)
api_bp.register_blueprint(api_doc_bp)
api_bp.register_blueprint(algorithm_bp)
