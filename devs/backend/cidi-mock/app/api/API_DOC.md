# 景区拥挤指数计算 API 文档

## 概述

本模块提供基于数学建模的景区拥挤指数计算接口，支持单景区、单区域和多景区联动的拥挤指数计算。

## 基础信息

- **Base URL**: `/algorithm`
- **Content-Type**: `application/json`
- **Method**: `POST`

---

## 接口列表

### 1. 计算景区拥挤指数

**接口路径**: `/algorithm/crowd_index`

**请求方法**: `POST`

**请求参数**:

```json
{
  "scenic_id": "scenic_001",
  "scenic_name": "示例景区",
  "zones": [
    {
      "zone_id": "core_1",
      "name": "核心景点",
      "zone_type": "CORE",
      "area": 1000.0,
      "max_density": 2.5,
      "service_rate": 3.0,
      "service_windows": 4,
      "space_form": "OPEN"
    }
  ],
  "zone_states": {
    "core_1": {
      "zone_id": "core_1",
      "timestamp": 0.0,
      "visitor_count": 500.0,
      "queue_count": 0.0,
      "weather_factor": 1.0,
      "event_factor": 1.0,
      "facility_factor": 1.0,
      "light_factor": 1.0
    }
  }
}
```

**参数说明**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| scenic_id | string | 否 | 景区ID，默认 "scenic_001" |
| scenic_name | string | 否 | 景区名称，默认 "示例景区" |
| zones | array | 是 | 区域配置数组 |
| zone_id | string | 是 | 区域唯一标识 |
| name | string | 是 | 区域名称 |
| zone_type | string | 是 | 区域类型：ENTRY, EXIT, PATH, CORE, RIDE, EXHIBIT, DINING, TOILET, SHOP, TRANSIT, PARKING |
| area | float | 是 | 区域有效面积（㎡） |
| max_density | float | 是 | 最大舒适密度（人/㎡） |
| service_rate | float | 否 | 服务速率（人/分钟/窗口），仅服务区域需要 |
| service_windows | int | 否 | 服务窗口数，仅服务区域需要 |
| space_form | string | 否 | 空间形态：OPEN, SEMI_ENCLOSED, ENCLOSED |
| zone_states | object | 是 | 区域状态字典 |
| timestamp | float | 否 | 时间戳（分钟），默认 0.0 |
| visitor_count | float | 是 | 实时游客数量 |
| queue_count | float | 否 | 排队人数，默认 0.0 |
| weather_factor | float | 否 | 天气折减系数，默认 1.0 |
| event_factor | float | 否 | 活动占用折减系数，默认 1.0 |
| facility_factor | float | 否 | 设施运行折减系数，默认 1.0 |
| light_factor | float | 否 | 照明折减系数，默认 1.0 |

**响应示例**:

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "scenic_id": "scenic_001",
    "timestamp": 0.0,
    "crowding_index": 0.3250,
    "crowding_level": "MODERATE",
    "base_index": 0.2500,
    "peak_index": 0.3500,
    "coupling_factor": 1.0000,
    "zone_results": {
      "core_1": {
        "zone_id": "core_1",
        "timestamp": 0.0,
        "visitor_count": 500.0,
        "effective_capacity": 2312.5,
        "load_ratio": 0.6500,
        "base_crowding": 0.3000,
        "bottleneck_factor": 1.0000,
        "queue_contribution": 0.0000,
        "propagation_contribution": 0.0250,
        "crowding_index": 0.3250,
        "crowding_level": "MODERATE"
      }
    }
  }
}
```

**响应字段说明**:

| 字段 | 类型 | 说明 |
|------|------|------|
| crowding_index | float | 景区整体拥挤指数 [0, 1] |
| crowding_level | string | 拥挤等级：COMFORTABLE(I), MODERATE(II), CROWDED(III), VERY_CROWDED(IV), SEVERE(V) |
| base_index | float | 基础指数（加权平均） |
| peak_index | float | 峰值指数（关键区域最大值） |
| coupling_factor | float | 耦合放大因子 |
| zone_results | object | 各区域计算结果 |

---

### 2. 计算单个区域拥挤指数

**接口路径**: `/algorithm/zone_crowding`

**请求方法**: `POST`

**请求参数**:

```json
{
  "zone": {
    "zone_id": "core_1",
    "name": "核心景点",
    "zone_type": "CORE",
    "area": 1000.0,
    "max_density": 2.5,
    "service_rate": 3.0,
    "service_windows": 4,
    "space_form": "OPEN"
  },
  "state": {
    "zone_id": "core_1",
    "timestamp": 0.0,
    "visitor_count": 500.0,
    "queue_count": 0.0,
    "weather_factor": 1.0,
    "event_factor": 1.0,
    "facility_factor": 1.0,
    "light_factor": 1.0
  },
  "upstream_crowding": {
    "upstream_1": 0.75
  }
}
```

**参数说明**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| zone | object | 是 | 区域配置对象 |
| state | object | 是 | 区域状态对象 |
| upstream_crowding | object | 否 | 上游区域拥挤指数字典 |

**响应示例**:

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "zone_id": "core_1",
    "timestamp": 0.0,
    "visitor_count": 500.0,
    "effective_capacity": 2312.5,
    "load_ratio": 0.6500,
    "base_crowding": 0.3000,
    "bottleneck_factor": 1.0000,
    "queue_contribution": 0.0000,
    "propagation_contribution": 0.0250,
    "crowding_index": 0.3250,
    "crowding_level": "MODERATE"
  }
}
```

---

### 3. 多景区联动计算

**接口路径**: `/algorithm/multi_scenic`

**请求方法**: `POST`

**请求参数**:

```json
{
  "scenic_areas": {
    "A": {
      "scenic_id": "A",
      "name": "热门景区A",
      "attraction_score": 90,
      "total_capacity": 10000,
      "location": [116.4, 39.9]
    },
    "B": {
      "scenic_id": "B",
      "name": "次热门景区B",
      "attraction_score": 75,
      "total_capacity": 8000,
      "location": [117.0, 39.5]
    }
  },
  "scenic_states": {
    "A": {
      "scenic_id": "A",
      "timestamp": 0.0,
      "crowding_index": 0.85
    },
    "B": {
      "scenic_id": "B",
      "timestamp": 0.0,
      "crowding_index": 0.45
    }
  },
  "visitor_counts": {
    "A": 12000,
    "B": 4000
  }
}
```

**参数说明**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| scenic_areas | object | 是 | 景区属性字典 |
| scenic_id | string | 是 | 景区ID |
| name | string | 是 | 景区名称 |
| attraction_score | int | 是 | 吸引力评分 [0, 100] |
| total_capacity | int | 是 | 景区总承载量 |
| location | array | 是 | 景区位置 [经度, 纬度] |
| scenic_states | object | 是 | 景区状态字典 |
| crowding_index | float | 是 | 景区拥挤指数 |
| visitor_counts | object | 是 | 各景区当前游客数 |

**响应示例**:

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "timestamp": 0.0,
    "scenic_indices": {
      "A": 0.85,
      "B": 0.45
    },
    "remaining_capacities": {
      "A": -2000,
      "B": 4000
    },
    "spill_over_flows": {
      "A->B": 0.0,
      "A->C": 600.0
    },
    "recommendations": {
      "A": ["C", "B"]
    }
  }
}
```

**响应字段说明**:

| 字段 | 类型 | 说明 |
|------|------|------|
| scenic_indices | object | 各景区拥挤指数 |
| remaining_capacities | object | 各景区剩余容量（负值表示超载） |
| spill_over_flows | object | 景区间溢出客流 |
| recommendations | object | 分流推荐 |

---

## 错误响应

所有接口在发生错误时返回：

```json
{
  "code": 500,
  "message": "Error: 具体错误信息",
  "data": null
}
```

---

## 使用示例

### Python 示例

```python
import requests
import json

# 1. 计算景区拥挤指数
url = "http://localhost:5000/algorithm/crowd_index"
data = {
    "scenic_id": "scenic_001",
    "scenic_name": "示例景区",
    "zones": [
        {
            "zone_id": "core_1",
            "name": "核心景点",
            "zone_type": "CORE",
            "area": 1000.0,
            "max_density": 2.5,
            "space_form": "OPEN"
        }
    ],
    "zone_states": {
        "core_1": {
            "zone_id": "core_1",
            "timestamp": 0.0,
            "visitor_count": 500.0,
            "weather_factor": 1.0
        }
    }
}

response = requests.post(url, json=data)
result = response.json()
print(json.dumps(result, indent=2, ensure_ascii=False))

# 2. 计算单区域拥挤指数
url = "http://localhost:5000/algorithm/zone_crowding"
data = {
    "zone": {
        "zone_id": "core_1",
        "name": "核心景点",
        "zone_type": "CORE",
        "area": 1000.0,
        "max_density": 2.5,
        "space_form": "OPEN"
    },
    "state": {
        "zone_id": "core_1",
        "timestamp": 0.0,
        "visitor_count": 500.0,
        "weather_factor": 1.0
    }
}

response = requests.post(url, json=data)
result = response.json()
print(json.dumps(result, indent=2, ensure_ascii=False))

# 3. 多景区联动
url = "http://localhost:5000/algorithm/multi_scenic"
data = {
    "scenic_areas": {
        "A": {
            "scenic_id": "A",
            "name": "热门景区A",
            "attraction_score": 90,
            "total_capacity": 10000,
            "location": [116.4, 39.9]
        },
        "B": {
            "scenic_id": "B",
            "name": "次热门景区B",
            "attraction_score": 75,
            "total_capacity": 8000,
            "location": [117.0, 39.5]
        }
    },
    "scenic_states": {
        "A": {
            "scenic_id": "A",
            "timestamp": 0.0,
            "crowding_index": 0.85
        },
        "B": {
            "scenic_id": "B",
            "timestamp": 0.0,
            "crowding_index": 0.45
        }
    },
    "visitor_counts": {
        "A": 12000,
        "B": 4000
    }
}

response = requests.post(url, json=data)
result = response.json()
print(json.dumps(result, indent=2, ensure_ascii=False))
```

### cURL 示例

```bash
# 计算景区拥挤指数
curl -X POST http://localhost:5000/algorithm/crowd_index \
  -H "Content-Type: application/json" \
  -d '{
    "scenic_id": "scenic_001",
    "scenic_name": "示例景区",
    "zones": [
      {
        "zone_id": "core_1",
        "name": "核心景点",
        "zone_type": "CORE",
        "area": 1000.0,
        "max_density": 2.5,
        "space_form": "OPEN"
      }
    ],
    "zone_states": {
      "core_1": {
        "zone_id": "core_1",
        "timestamp": 0.0,
        "visitor_count": 500.0,
        "weather_factor": 1.0
      }
    }
  }'
```

---

## 拥挤等级说明

| 等级 | 名称 | 指数范围 | 说明 |
|------|------|----------|------|
| I | 舒适 | [0, 0.25) | 游客稀少，体验良好 |
| II | 适中 | [0.25, 0.5) | 游客适中，体验正常 |
| III | 拥挤 | [0.5, 0.75) | 游客较多，体验下降 |
| IV | 非常拥挤 | [0.75, 0.9) | 游客密集，体验较差 |
| V | 严重拥挤 | [0.9, 1.0] | 游客超载，体验极差 |

---

## 天气折减系数说明

| 天气类型 | 折减系数 | 适用场景 |
|----------|----------|----------|
| 晴天/多云 | 1.0 | 室外区域正常承载力 |
| 小雨/微风 | 0.85 | 室外区域承载力下降15% |
| 中雨/高温(>38°C) | 0.55 | 室外区域承载力下降45% |
| 暴雨/暴雪/极端 | 0.35 | 室外区域承载力下降65% |

---

## 技术支持

如有问题，请联系开发团队或参考：
- [景点游客拥挤指数数学建模.md](../../../docs/03-model-doc/景点游客拥挤指数数学建模.md)
- [kualk模块README](../algorithm/kualk/README.md)
