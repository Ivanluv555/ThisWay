"""cidi-model FastAPI — 配置"""
import os

# Spring Boot 基础地址，默认本地
SPRING_BASE_URL: str = os.environ.get("SPRING_BASE_URL", "http://localhost:8080")

# 随机种子（保持预测可复现）
RANDOM_SEED: int = int(os.environ.get("SCENIC_SEED", "42"))

# 历史数据往前追溯的小时数（生产使用7天）
HISTORY_HOURS: int = int(os.environ.get("HISTORY_HOURS", "168"))

# MySQL 连接配置（用于 spider 景点同步）
MYSQL_HOST: str = os.environ.get("MYSQL_HOST", "127.0.0.1")
MYSQL_PORT: int = int(os.environ.get("MYSQL_PORT", "3306"))
MYSQL_USER: str = os.environ.get("MYSQL_USER", "root")
MYSQL_PASSWORD: str = os.environ.get("MYSQL_PASSWORD", "")
MYSQL_DATABASE: str = os.environ.get("MYSQL_DATABASE", "cidi_db")
MYSQL_TABLE_SPIDER_SPOTS: str = os.environ.get("MYSQL_TABLE_SPIDER_SPOTS", "spider_regional_scenic_spots")
MYSQL_TABLE_SPIDER_SPOT_DETAILS: str = os.environ.get("MYSQL_TABLE_SPIDER_SPOT_DETAILS", "spider_scenic_spot_details")

# 景点详情同步节流配置（避免请求过密触发风控）
SPOT_DETAIL_SYNC_DELAY_SECONDS: float = float(os.environ.get("SPOT_DETAIL_SYNC_DELAY_SECONDS", "1.5"))
SPOT_DETAIL_SYNC_JITTER_SECONDS: float = float(os.environ.get("SPOT_DETAIL_SYNC_JITTER_SECONDS", "0.8"))
