from flask import Blueprint, send_file
from pathlib import Path

templates_path = Path(__file__).parent.parent / 'static' / 'templates'
api_doc_bp = Blueprint('api_doc', __name__, template_folder=templates_path)

@api_doc_bp.route('/doc')
def api_doc():
    return send_file(templates_path / 'api_doc.html')
