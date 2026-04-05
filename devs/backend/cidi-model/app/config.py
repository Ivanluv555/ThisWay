"""cidi-model FastAPI — 配置"""
import os

# Spring Boot 基础地址，默认本地
SPRING_BASE_URL: str = os.environ.get("SPRING_BASE_URL", "http://localhost:8080")

# 随机种子（保持预测可复现）
RANDOM_SEED: int = int(os.environ.get("SCENIC_SEED", "42"))

# 历史数据往前追溯的小时数（生产使用7天）
HISTORY_HOURS: int = int(os.environ.get("HISTORY_HOURS", "168"))
