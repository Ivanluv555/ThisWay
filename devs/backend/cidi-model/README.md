# cidi-model 人流预测服务

Python 建模算法后端，基于 FastAPI 提供景区人流预测与拥挤指数计算服务。

## 项目结构

```
cidi-model/
├── app/                          # 应用主模块
│   ├── algorithms/               # 核心算法
│   │   ├── __init__.py
│   │   └── crowd_index_calc.py   # 拥挤指数计算模型
│   ├── services/                 # 业务服务层
│   │   ├── crowd_index_service.py      # 拥挤指数服务
│   │   ├── district_lookup_service.py  # 地区查询服务
│   │   ├── flow_simulation_service.py  # 客流模拟服务
│   │   ├── spot_detail_sync_service.py # 景点详情同步
│   │   ├── spot_sync_service.py        # 景点同步服务
│   │   └── visitor_profile_service.py  # 游客画像服务
│   ├── spider/                   # 数据爬虫
│   │   ├── xc_spot_detail.py     # 携程景点详情爬虫
│   │   └── xc_spot_search.py     # 携程景点搜索爬虫
│   ├── config.py                 # 配置管理
│   ├── models.py                 # 数据模型
│   ├── predictor.py              # LSTM 预测模型
│   └── spring_client.py          # Spring Boot 客户端
├── data/                         # 数据文件
│   └── mock_spots.json           # 模拟景点数据
├── tests/                        # 测试模块
│   ├── __init__.py
│   └── smoke_test.py             # 集成测试
├── main.py                       # FastAPI 应用入口
├── requirements.txt              # 依赖列表
└── README.md                     # 项目文档
```

## 快速开始

### 1. 安装依赖

```bash
pip install -r requirements.txt
```

### 2. 配置环境变量

```bash
# Spring Boot 后端地址
export SPRING_BASE_URL=http://localhost:8080

# MySQL 数据库配置
export MYSQL_HOST=127.0.0.1
export MYSQL_PORT=3306
export MYSQL_USER=root
export MYSQL_PASSWORD=your_password
export MYSQL_DATABASE=cidi_db

# 可选配置
export SCENIC_SEED=42                    # 随机种子
export HISTORY_HOURS=168                 # 历史数据窗口（小时）
```

### 3. 启动服务

```bash
cd /path/to/cidi-model
uvicorn main:app --host 0.0.0.0 --port 8001 --reload
```

访问 API 文档：http://localhost:8001/docs

## 核心功能

### 1. 定时预测服务 (`POST /prediction/scheduler/toggle`) ⭐ 新增
- **每整点自动触发**人流预测
- 从 Spring Boot 获取历史数据
- 基于 LSTM 模型预测下一小时客流
- 自动生成分流策略建议
- 将预测结果写回数据库
- 支持启停控制和状态查询

详细文档：[定时预测服务使用指南](./docs/PREDICTION_SCHEDULER.md)

### 2. 手动预测 (`POST /predict`)
- 基于 LSTM 模型预测下一小时景区客流
- 自动生成分流策略建议
- 支持历史数据不足时自动模拟补齐

### 3. 拥挤指数计算 (`GET /crowd-index`)
- 基于区域承载力模型计算实时拥挤指数
- 支持多功能区综合评估
- 考虑天气、管控状态等外部因素

### 4. 客流模拟 (`POST /flow/simulation/toggle`)
- 定时生成模拟客流数据
- 支持启停控制
- 可批量生成历史/未来数据

### 5. 景点数据采集
- 携程景点搜索与详情爬取
- 自动同步到数据库
- 支持地区级批量同步

## 快速使用

### 启动定时预测服务

```bash
# 启动定时预测（每整点自动预测）
curl -X POST "http://localhost:8001/prediction/scheduler/toggle?enabled=true"

# 查看状态
curl "http://localhost:8001/prediction/scheduler/status"

# 立即执行一次预测（不等整点）
curl -X POST "http://localhost:8001/prediction/run-now"
```

### 启动客流模拟服务

```bash
# 启动模拟上报（每 60 秒生成一次数据）
curl -X POST "http://localhost:8001/flow/simulation/toggle?enabled=true"

# 查看状态
curl "http://localhost:8001/flow/simulation/status"
```

详细使用说明请查看 [定时预测服务文档](./docs/PREDICTION_SCHEDULER.md)。

## 测试

运行集成测试：

```bash
python -m tests.smoke_test
```

## 文档

- [项目架构文档](./ARCHITECTURE.md) - 模块设计、数据流、依赖关系
- [重构记录](./REFACTORING.md) - 项目重构历史
- [定时预测服务](./docs/PREDICTION_SCHEDULER.md) - 定时预测服务使用指南

## 技术栈

- **Web 框架**: FastAPI
- **机器学习**: PyTorch (LSTM)
- **数据处理**: Pandas, NumPy
- **HTTP 客户端**: httpx
- **数据库**: MySQL (通过 Spring Boot API)

## 开发说明

- 代码结构遵循模块化设计，便于扩展和维护
- 所有服务通过 Spring Boot API 与数据库交互
- 算法模块独立封装，可单独测试和复用
