from app import create_app
from app.utils.config import load_config

app = create_app()

if __name__ == '__main__':
    cfg = load_config()
    _host = cfg['server']['host']
    _port = cfg['server']['port']

    app.run(host=_host, port=_port, debug=True)

