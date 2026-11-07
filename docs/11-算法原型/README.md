## 11 算法原型

本目录保留原 `docs/03-model-doc/code` 的六份 Python 文件。它们实现旧复杂模型：区域负载、非线性映射、瓶颈、排队、通道传播、景区聚合与多景区联动。

当前 FastAPI 使用的简化区域模型位于 [crowd_index_calc.py](../../backend/cidi-model/app/algorithms/crowd_index_calc.py)，公式、输入与量纲见 [05 模型与预测](../05-模型与预测.md)。两个版本不能互换。

| 文件 | 职责 |
| --- | --- |
| `models.py` | Zone、Corridor、ScenicArea、状态与等级 |
| `core.py` | 非线性映射、排队、瓶颈、传播与聚合 |
| `multi_scenic.py` | 景区属性、转移概率、溢出与推荐 |
| `__init__.py` | 包导出 |
| `demo.py` | 典型场景演示 |
| `test_models.py` | 原型单元测试 |

从仓库根目录运行，需先在隔离 Python 环境安装 NumPy：

```bash
python docs/11-算法原型/demo.py
python docs/11-算法原型/test_models.py
```

此前“仅标准库”的说明已纠正；基础环境未安装 NumPy，前次测试停在导入阶段。`cidi-mock/app/algorithm/kualk` 仍有独立服务使用的算法副本；六个 Python 文件中四份内容相同、`core.py` 和 `__init__.py` 存在差异。本次移动不合并它们的业务实现。
