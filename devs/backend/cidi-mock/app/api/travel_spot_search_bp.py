from flask import Blueprint

travel_spot_search_bp = Blueprint('travel_spot_search', __name__)

@travel_spot_search_bp.route('/search')
def search():
    return "search"


