# 定时预测服务使用指南

## 功能说明

定时预测服务会**每整点自动触发**人流预测，并将结果写回数据库。

### 工作流程

```
每小时整点 (00:00, 01:00, 02:00, ...)
    ↓
从 Spring Boot 获取历史数据
    ↓
LSTM 模型训练与预测
    ↓
计算分流策略
    ↓
写回预测结果到 Spring Boot
    ↓
等待下一个整点
```

## API 接口

### 1. 启动定时预测服务

```bash
POST /prediction/scheduler/toggle?enabled=true
```

**响应示例：**
```json
{
  "running": true,
  "stopping": false,
  "startedAt": "2026-05-07T18:00:00",
  "lastPredictAt": null,
  "lastError": null,
  "predictionCount": 0,
  "lastSummary": null,
  "nextScheduledHour": "2026-05-07T19:00:00"
}
```

### 2. 停止定时预测服务

```bash
POST /prediction/scheduler/toggle?enabled=false
```

### 3. 查询服务状态

```bash
GET /prediction/scheduler/status
```

**响应示例（运行中）：**
```json
{
  "running": true,
  "stopping": false,
  "startedAt": "2026-05-07T18:00:00",
  "lastPredictAt": "2026-05-07T19:00:15",
  "lastError": null,
  "predictionCount": 3,
  "lastSummary": {
    "predictTime": "2026-05-07T20:00:00",
    "spotCount": 21,
    "peakCiBefore": 7.8,
    "peakCiAfter": 6.2,
    "overloadedBefore": 5,
    "overloadedAfter": 2,
    "diversionPairs": 8
  },
  "nextScheduledHour": "2026-05-07T20:00:00"
}
```

### 4. 立即执行预测（手动触发）

```bash
POST /prediction/run-now
```

不等待整点，立即执行一次预测。

## 使用场景

### 场景 1：开发测试

```bash
# 1. 启动定时预测服务
curl -X POST "http://localhost:8001/prediction/scheduler/toggle?enabled=true"

# 2. 立即执行一次预测（不等整点）
curl -X POST "http://localhost:8001/prediction/run-now"

# 3. 查看状态
curl "http://localhost:8001/prediction/scheduler/status"

# 4. 停止服务
curl -X POST "http://localhost:8001/prediction/scheduler/toggle?enabled=false"
```

### 场景 2：生产环境

**方式 A：手动启动**
```bash
# 启动 FastAPI 服务后，手动调用 API 启动定时预测
curl -X POST "http://localhost:8001/prediction/scheduler/toggle?enabled=true"
```

**方式 B：自动启动（推荐）**

在 `main.py` 添加启动事件：

```python
@app.on_event("startup")
async def startup_event():
    # 自动启动定时预测服务
    await start_prediction_scheduler()
    logger.info("定时预测服务已自动启动")
```

## 配置参数

通过环境变量配置：

```bash
# 历史数据窗口（小时）
export HISTORY_HOURS=168

# 随机种子（保证可复现）
export SCENIC_SEED=42

# Spring Boot 地址
export SPRING_BASE_URL=http://localhost:8080
```

## 监控与日志

### 查看日志

```bash
# 服务启动日志
[INFO] 定时预测服务已启动

# 整点触发日志
[INFO] 检测到整点，开始执行预测...
[INFO] 获取到 21 个景点
[INFO] 开始训练 LSTM 模型...
[INFO] 预测完成，Top3: [(1, 2500.5), (2, 2300.2), (3, 2100.8)]
[INFO] 写入预测结果 21 条
[INFO] 写入转移预测 8 条
[INFO] 定时预测完成: count=1, predictTime=2026-05-07T19:00:00, spots=21, diversionPairs=8

# 错误日志
[ERROR] 定时预测失败: 无法连接到 Spring Boot 服务
```

### 状态字段说明

| 字段 | 说明 |
|------|------|
| `running` | 服务是否正在运行 |
| `stopping` | 服务是否正在停止 |
| `startedAt` | 服务启动时间 |
| `lastPredictAt` | 上次预测执行时间 |
| `lastError` | 上次错误信息（无错误则为 null） |
| `predictionCount` | 累计预测次数 |
| `lastSummary` | 上次预测结果摘要 |
| `nextScheduledHour` | 下次计划执行时间 |

## 与模拟服务的区别

| 特性 | 模拟服务 | 定时预测服务 |
|------|---------|-------------|
| **功能** | 生成模拟客流数据 | 基于历史数据预测未来客流 |
| **触发频率** | 每 60 秒 | 每整点（每小时） |
| **数据来源** | 随机生成 | LSTM 模型预测 |
| **写入表** | `location_hourly_flow` | `spot_traffic_predictions` |
| **用途** | 开发测试、数据填充 | 生产预测、决策支持 |
| **启动接口** | `POST /flow/simulation/toggle` | `POST /prediction/scheduler/toggle` |

## 常见问题

### Q1: 服务启动后多久会执行第一次预测？

A: 在下一个整点执行。例如 18:30 启动，会在 19:00 执行第一次预测。

### Q2: 如果想立即执行怎么办？

A: 调用 `POST /prediction/run-now` 手动触发。

### Q3: 预测失败会怎样？

A: 记录错误日志，但服务继续运行，会在下一个整点重试。

### Q4: 历史数据不足怎么办？

A: 服务会自动生成模拟数据补齐，并写回数据库建立基线。

### Q5: 可以同时运行模拟服务和预测服务吗？

A: 可以。模拟服务生成当前数据，预测服务基于历史数据预测未来。

## 最佳实践

1. **生产环境**：启用自动启动，确保服务重启后自动恢复
2. **开发环境**：手动启动，便于调试和测试
3. **监控告警**：定期检查 `lastError` 字段，发现异常及时处理
4. **数据质量**：确保 Spring Boot 服务稳定，历史数据完整
5. **资源管理**：预测任务消耗 CPU，建议在低峰期启动服务

## 技术细节

- **调度机制**：每 30 秒检查一次是否到达整点
- **去重逻辑**：同一小时内只执行一次预测
- **异步执行**：预测任务在后台异步运行，不阻塞 API 响应
- **错误恢复**：单次失败不影响后续执行
- **模型训练**：每次预测都会重新训练 LSTM 模型（60 epochs）
