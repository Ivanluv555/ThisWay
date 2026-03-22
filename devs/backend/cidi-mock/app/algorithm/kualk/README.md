# 景区游客拥挤指数数学模型

## 概述

本模块包含基于数学建模的完整拥挤指数计算模型，采用分层递进架构，支持多维度、多场景的拥挤指数计算。

## 目录结构

```
kualk/
├── __init__.py              # 模块入口
├── models.py              # 数据结构定义
├── core.py                # 核心计算模块
├── multi_scenic.py        # 多景区联动模块
├── test_models.py         # 测试用例
└── demo.py                # 效果演示脚本
```

## 模型特性

| 特性 | 说明 |
|------|------|
| **建模思路** | 分层递进架构（区域级 → 景区级 → 多景区级） |
| **输入参数** | 区域拓扑、实时状态、天气、排队等 |
| **计算维度** | 负载比、非线性映射、瓶颈、排队、传导 |
| **感知权重** | 基于区域类型的差异化权重 |
| **拥挤等级** | 5级（I-V级） |
| **多景区联动** | 支持溢出效应和分流推荐 |
| **天气影响** | 支持动态折减系数 |
| **瓶颈效应** | 支持超线性放大 |
| **排队感知** | 支持阈值效应 |

## 使用示例

### 基础使用

```python
from kualk import CrowdingIndexModel, ScenicArea, Zone, ZoneState, ZoneType

model = CrowdingIndexModel()

scenic = ScenicArea(scenic_id="scenic_001", name="示例景区")

zone = Zone(
    zone_id="core_1",
    name="核心景点",
    zone_type=ZoneType.CORE,
    area=1000.0,
    max_density=2.5
)

scenic.add_zone(zone)

state = ZoneState(
    zone_id="core_1",
    timestamp=0.0,
    visitor_count=500.0,
    weather_factor=1.0
)

result = model.compute_scenic_crowding(scenic, {zone.zone_id: state})

print(f"景区拥挤指数: {result.crowding_index:.4f}")
print(f"拥挤等级: {result.crowding_level.value}")
```

### 完整景区计算

```python
from kualk import CrowdingIndexModel, ScenicArea, Zone, ZoneState, ZoneType, SpaceForm

model = CrowdingIndexModel()

scenic = ScenicArea(scenic_id="scenic_001", name="示例风景区")

zones = [
    Zone("entry_1", "入口检票区", ZoneType.ENTRY, 300.0, 2.0, 
         service_rate=3.0, service_windows=4, space_form=SpaceForm.OPEN),
    Zone("core_1", "主峰观景台", ZoneType.CORE, 800.0, 2.5, space_form=SpaceForm.OPEN),
    Zone("path_1", "主游览步道", ZoneType.PATH, 1000.0, 1.5, space_form=SpaceForm.OPEN),
    Zone("dining_1", "餐饮休息区", ZoneType.DINING, 400.0, 1.5, space_form=SpaceForm.SEMI_CLOSED),
]

for zone in zones:
    scenic.add_zone(zone)

zone_states = {
    "entry_1": ZoneState("entry_1", 0.0, 160.0, queue_count=20.0),
    "core_1": ZoneState("core_1", 0.0, 320.0),
    "path_1": ZoneState("path_1", 0.0, 435.0),
    "dining_1": ZoneState("dining_1", 0.0, 132.0, queue_count=10.0),
}

result = model.compute_scenic_crowding(scenic, zone_states)

print(f"景区整体拥挤指数：{result.crowding_index:.4f}")
print(f"拥挤等级：{result.crowding_level.value}")
print(f"基础指数：{result.base_index:.4f}")
print(f"峰值指数：{result.peak_index:.4f}")
print(f"耦合放大因子：{result.coupling_factor:.4f}")

for zone_id, zone_result in result.zone_results.items():
    print(f"{zone_id}: CI={zone_result.crowding_index:.4f}, 负载比={zone_result.load_ratio:.2f}")
```

### 多景区联动

```python
from kualk import MultiScenicCoordinator, ScenicAttributes, ScenicCrowdingResult

coordinator = MultiScenicCoordinator()

scenic_attrs = {
    "A": ScenicAttributes(
        scenic_id="A",
        name="热门景区A",
        attraction_score=90,
        total_capacity=10000,
        location=(116.4, 39.9)
    ),
    "B": ScenicAttributes(
        scenic_id="B",
        name="次热门景区B",
        attraction_score=75,
        total_capacity=8000,
        location=(117.0, 39.5)
    ),
}

for scenic_id, attrs in scenic_attrs.items():
    coordinator.add_scenic_area(attrs)

coordinator.update_scenic_state("A", ScenicCrowdingResult("A", 0.0, 0.85), 12000)
coordinator.update_scenic_state("B", ScenicCrowdingResult("B", 0.0, 0.45), 4000)

state_matrix = coordinator.get_state_matrix(0.0)
recommendations = coordinator.get_recommendations()

print(f"各景区拥挤指数：{state_matrix.scenic_indices}")
print(f"各景区剩余容量：{state_matrix.remaining_capacities}")
print(f"溢出客流：{state_matrix.spill_over_flows}")
print(f"分流推荐：{recommendations}")
```

## 运行演示脚本

```bash
cd m:\我的桌面\待作项目\UIEP\A项目仓库\devs\backend\cidi-mock\app\algorithm\kualk
python demo.py
```

演示脚本包含以下内容：
1. 非线性映射函数 Φ(r) 演示
2. 排队等待修正 Ψ(W) 演示
3. 瓶颈效应放大演示
4. 区域拥挤指数计算演示
5. 服务区域排队演示
6. 天气折减效应演示
7. 完整景区拥挤指数计算演示（低/高客流场景）
8. 多景区联动演示

## 运行测试

```bash
cd m:\我的桌面\待作项目\UIEP\A项目仓库\devs\backend\cidi-mock\app\algorithm\kualk
python test_models.py
```

## 文档引用

详细建模思路和公式推导请参阅：
- [景点游客拥挤指数数学建模.md](../../../docs/03-model-doc/景点游客拥挤指数数学建模.md)
