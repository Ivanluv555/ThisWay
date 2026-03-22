# 景区游客拥挤指数数学模型 - 代码实现

## 概述

本代码库实现了《景点游客拥挤指数数学建模》文档中描述的完整数学模型体系，包括：

- **区域级拥挤指数模型**：负载比计算、非线性映射、排队修正、瓶颈放大
- **景区级拥挤指数聚合**：感知权重聚合、耦合放大修正
- **多景区联动模型**：溢出效应、转移概率、协同调度

## 目录结构

```
code/
├── __init__.py          # 包入口
├── models.py            # 数据结构定义
├── core.py              # 核心计算模块
├── multi_scenic.py      # 多景区联动模块
├── test_models.py       # 测试用例
└── README.md            # 本文档
```

## 快速开始

### 1. 创建景区和区域

```python
from code import ScenicArea, Zone, ZoneType, SpaceForm

scenic = ScenicArea(
    scenic_id="scenic_001",
    name="示例景区"
)

core_zone = Zone(
    zone_id="core_1",
    name="核心景点",
    zone_type=ZoneType.CORE,
    area=1000.0,              # 有效面积 1000㎡
    max_density=2.5,          # 最大舒适密度 2.5人/㎡
    space_form=SpaceForm.OPEN  # 开敞室外空间
)

entry_zone = Zone(
    zone_id="entry_1",
    name="入口检票",
    zone_type=ZoneType.ENTRY,
    area=200.0,
    service_rate=3.0,         # 服务速率 3人/分钟/窗口
    service_windows=4,        # 4个检票窗口
    space_form=SpaceForm.OPEN
)

scenic.add_zone(core_zone)
scenic.add_zone(entry_zone)
```

### 2. 创建通道

```python
from code import Corridor

corridor = Corridor(
    corridor_id="c1",
    from_zone="entry_1",
    to_zone="core_1",
    length=100.0,    # 长度100米
    width=3.0        # 宽度3米
)

scenic.add_corridor(corridor)
```

### 3. 设置区域状态

```python
from code import ZoneState

zone_states = {
    "core_1": ZoneState(
        zone_id="core_1",
        timestamp=10.0,           # 时间戳（分钟）
        visitor_count=1500.0,     # 当前游客数
        weather_factor=1.0        # 天气折减系数
    ),
    "entry_1": ZoneState(
        zone_id="entry_1",
        timestamp=10.0,
        visitor_count=300.0,
        queue_count=60.0          # 排队人数
    )
}
```

### 4. 计算拥挤指数

```python
from code import CrowdingIndexModel

model = CrowdingIndexModel()

result = model.compute_scenic_crowding(scenic, zone_states)

print(f"景区拥挤指数: {result.crowding_index:.2f}")
print(f"拥挤等级: {result.crowding_level.value}")
```

## 核心公式对照

代码实现与文档公式的对应关系：

| 公式编号 | 公式 | 代码实现 |
|---------|------|----------|
| F1 | $r_i(t) = N_i(t) / C_i^{eff}(t)$ | `ZoneLoadRatioCalculator.compute_load_ratio()` |
| F2 | $C_i^{eff} = C_i^{max} \cdot \kappa_i \cdot \prod \eta_k$ | `ZoneLoadRatioCalculator.compute_effective_capacity()` |
| F3 | $\Phi(r)$ 非线性映射 | `NonlinearMappingFunction.compute()` |
| F4 | $B_i$ 瓶颈放大因子 | `BottleneckAmplificationFactor.compute()` |
| F5 | $\Psi(W_i)$ 排队等待修正 | `QueueWaitingCorrection.compute()` |
| F6 | $\Delta CI_i^{prop}$ 拥挤传导项 | `CrowdingPropagation.compute()` |
| F7 | $CI_i$ 区域拥挤指数 | `ZoneCrowdingIndexCalculator.compute()` |
| F8-F11 | $SCI_s$ 景区拥挤指数 | `ScenicCrowdingAggregator.aggregate()` |

## 参数配置

### 非线性映射参数

```python
from code import MappingParams, CrowdingIndexModel

params = MappingParams(
    r1=0.6,       # 舒适→中度临界
    r2=0.8,       # 中度→重度临界
    r3=1.2,       # 极端拥挤饱和点
    phi1=0.30,    # r1处拥挤指数
    phi2=0.70,    # r2处拥挤指数
    p=2.0,        # 超线性增长指数
    lambda_=5.0   # 指数增长速率
)

model = CrowdingIndexModel(mapping_params=params)
```

### 排队等待参数

```python
from code import QueueParams

params = QueueParams(
    w_thresh=30.0,  # 等待容忍阈值（分钟）
    psi_0=0.15,     # 阈值内最大贡献
    psi_1=0.20      # 超阈值增长系数
)
```

### 瓶颈放大参数

```python
from code import BottleneckParams

params = BottleneckParams(
    beta=1.75  # 放大指数（推荐1.5~2.0）
)
```

### 聚合参数

```python
from code import AggregationParams

params = AggregationParams(
    xi=0.6,           # 均值-峰值平衡系数
    ci_high_thresh=0.6,  # 高负载阈值
    delta=0.3,        # 耦合放大最大幅度
    q=1.5             # 耦合超线性指数
)
```

## 多景区联动

```python
from code import MultiScenicCoordinator, ScenicAttributes

coordinator = MultiScenicCoordinator()

attrs_a = ScenicAttributes(
    scenic_id="A",
    name="景区A",
    attractiveness=80,
    total_capacity=10000,
    location=(116.4, 39.9)  # 经纬度
)

coordinator.add_scenic_area(attrs_a, result_a, visitor_count_a)

state_matrix = coordinator.get_state_matrix(timestamp=10.0)

recommendations = coordinator.get_recommendations()
```

## 拥挤等级说明

| 等级 | 指数范围 | 状态描述 | 管理建议 |
|------|----------|----------|----------|
| I 级 | [0, 0.25) | 舒适 | 正常运营 |
| II 级 | [0.25, 0.50) | 一般 | 正常运营 |
| III 级 | [0.50, 0.70) | 拥挤 | 启动引导分流 |
| IV 级 | [0.70, 0.85) | 非常拥挤 | 启动限流措施 |
| V 级 | [0.85, 1.0] | 极度拥挤 | 关闭售票/告警 |

## 运行测试

```bash
cd m:\我的桌面\待作项目\UIEP\A项目仓库\docs\03-model-doc\code
python -m pytest test_models.py -v
```

或直接运行：

```bash
python test_models.py
```

## 依赖

- Python 3.8+
- 无外部依赖（仅使用标准库）

## 文档引用

详细建模思路和公式推导请参阅：
- [景点游客拥挤指数数学建模.md](../景点游客拥挤指数数学建模.md)
