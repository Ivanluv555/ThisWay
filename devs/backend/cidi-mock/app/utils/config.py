import pathlib
import yaml


def load_config():
    """加载全局环境配置"""
    base_dir = pathlib.Path(__file__).parent.parent.parent.absolute()  # 定位到项目根目录
    config_path = base_dir.joinpath("config.yaml")  # 获取配置文件路径
    if config_path.exists() and yaml is not None:
        return yaml.load(open(config_path, "r"), Loader=yaml.FullLoader)
    return {}


def get_config():
    """对外提供获取配置的接口"""
    return _global_config


# 无论多少个文件 import 此模块，这行代码只在第一次 import 时运行
_global_config = load_config()

if __name__ == '__main__':
    from pprint import pprint
    print("Loaded configuration:")
    pprint(_global_config)
