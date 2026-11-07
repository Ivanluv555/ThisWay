"""
景区游客拥挤指数数学模型 - 测试模块

本模块包含所有核心功能的测试用例
"""

import unittest
import math
import sys
import os
from typing import Dict

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from models import (
    Zone, ZoneType, SpaceForm, ZoneState,
    ScenicArea, Corridor,
    get_crowding_level, get_perception_weight,
    CrowdingLevel, WeatherType, get_weather_factor
)
from core import (
    MappingParams, NonlinearMappingFunction,
    QueueParams, QueueWaitingCorrection,
    BottleneckParams, BottleneckAmplificationFactor,
    PropagationParams, CrowdingPropagation,
    ZoneLoadRatioCalculator, ZoneCrowdingIndexCalculator,
    ScenicCrowdingAggregator, CrowdingIndexModel
)
from multi_scenic import (
    ScenicAttributes, SpilloverParams, TransferUtilityWeights,
    TransferProbabilityModel, SpilloverModel, MultiScenicCoordinator
)


class TestZoneAndCorridor(unittest.TestCase):
    """测试区域和通道数据结构"""

    def test_zone_creation(self):
        """测试区域创建"""
        zone = Zone(
            zone_id="core_1",
            name="主景点",
            zone_type=ZoneType.CORE,
            area=1000.0,
            max_density=2.5,
            space_form=SpaceForm.OPEN
        )

        self.assertEqual(zone.zone_id, "core_1")
        self.assertEqual(zone.zone_type, ZoneType.CORE)
        self.assertAlmostEqual(zone.kappa, 0.925, places=2)
        self.assertEqual(zone.max_capacity, 2500.0)
        self.assertTrue(zone.is_service_zone == False)

    def test_service_zone(self):
        """测试服务类区域"""
        zone = Zone(
            zone_id="toilet_1",
            name="卫生间A",
            zone_type=ZoneType.TOILET,
            area=100.0,
            service_rate=2.0,
            service_windows=3,
            space_form=SpaceForm.ENCLOSED
        )

        self.assertTrue(zone.is_service_zone)
        self.assertAlmostEqual(zone.kappa, 0.60, places=2)

    def test_corridor_creation(self):
        """测试通道创建"""
        corridor = Corridor(
            corridor_id="c1",
            from_zone="entry",
            to_zone="core",
            length=100.0,
            width=3.0
        )

        self.assertEqual(corridor.max_flow_rate, corridor.width * 1.3 * 1.25 * 60)

    def test_corridor_speed_density(self):
        """测试通道速度-密度关系"""
        corridor = Corridor(
            corridor_id="c1",
            from_zone="a",
            to_zone="b",
            length=100.0,
            width=3.0
        )

        v_free = corridor.compute_speed(0.0)
        self.assertAlmostEqual(v_free, 1.3, places=2)

        v_half = corridor.compute_speed(3.0)
        self.assertLess(v_half, v_free)
        self.assertGreater(v_half, 0)

        v_jam = corridor.compute_speed(6.0)
        self.assertAlmostEqual(v_jam, 0.0, places=2)


class TestPerceptionWeight(unittest.TestCase):
    """测试感知权重"""

    def test_core_weight(self):
        """核心景点权重最高"""
        self.assertEqual(get_perception_weight(ZoneType.CORE), 1.50)

    def test_toilet_weight(self):
        """卫生间权重较高"""
        self.assertEqual(get_perception_weight(ZoneType.TOILET), 1.30)

    def test_path_weight(self):
        """通道权重较低"""
        self.assertEqual(get_perception_weight(ZoneType.PATH), 0.80)

    def test_parking_weight(self):
        """停车场权重最低"""
        self.assertEqual(get_perception_weight(ZoneType.PARKING), 0.75)


class TestCrowdingLevel(unittest.TestCase):
    """测试拥挤等级划分"""

    def test_comfortable(self):
        """舒适等级"""
        self.assertEqual(get_crowding_level(0.0), CrowdingLevel.COMFORTABLE)
        self.assertEqual(get_crowding_level(0.24), CrowdingLevel.COMFORTABLE)

    def test_moderate(self):
        """一般等级"""
        self.assertEqual(get_crowding_level(0.25), CrowdingLevel.MODERATE)
        self.assertEqual(get_crowding_level(0.49), CrowdingLevel.MODERATE)

    def test_crowded(self):
        """拥挤等级"""
        self.assertEqual(get_crowding_level(0.50), CrowdingLevel.CROWDED)
        self.assertEqual(get_crowding_level(0.69), CrowdingLevel.CROWDED)

    def test_very_crowded(self):
        """非常拥挤等级"""
        self.assertEqual(get_crowding_level(0.70), CrowdingLevel.VERY_CROWDED)
        self.assertEqual(get_crowding_level(0.84), CrowdingLevel.VERY_CROWDED)

    def test_extreme(self):
        """极度拥挤等级"""
        self.assertEqual(get_crowding_level(0.85), CrowdingLevel.EXTREME)
        self.assertEqual(get_crowding_level(1.0), CrowdingLevel.EXTREME)


class TestWeatherFactor(unittest.TestCase):
    """测试天气折减系数"""

    def test_sunny(self):
        self.assertEqual(get_weather_factor(WeatherType.SUNNY), 1.0)

    def test_light_rain(self):
        self.assertEqual(get_weather_factor(WeatherType.LIGHT_RAIN), 0.80)

    def test_moderate_rain(self):
        self.assertEqual(get_weather_factor(WeatherType.MODERATE_RAIN), 0.55)

    def test_severe(self):
        self.assertEqual(get_weather_factor(WeatherType.SEVERE), 0.35)


class TestLoadRatioCalculator(unittest.TestCase):
    """测试负载比计算"""

    def setUp(self):
        self.zone = Zone(
            zone_id="test",
            name="测试区域",
            zone_type=ZoneType.PATH,
            area=500.0,
            max_density=2.0,
            space_form=SpaceForm.OPEN
        )

    def test_effective_capacity_normal(self):
        """正常情况下的有效承载力"""
        capacity = ZoneLoadRatioCalculator.compute_effective_capacity(
            zone=self.zone,
            weather_factor=1.0,
            event_factor=1.0,
            facility_factor=1.0,
            light_factor=1.0
        )

        expected = 500.0 * 2.0 * 0.925
        self.assertAlmostEqual(capacity, expected, places=2)

    def test_effective_capacity_with_weather(self):
        """天气折减下的有效承载力"""
        capacity = ZoneLoadRatioCalculator.compute_effective_capacity(
            zone=self.zone,
            weather_factor=0.55,
            event_factor=1.0,
            facility_factor=1.0,
            light_factor=1.0
        )

        base_capacity = 500.0 * 2.0 * 0.925
        expected = base_capacity * 0.55
        self.assertAlmostEqual(capacity, expected, places=2)

    def test_load_ratio_normal(self):
        """正常负载比计算"""
        ratio = ZoneLoadRatioCalculator.compute_load_ratio(500, 1000)
        self.assertEqual(ratio, 0.5)

    def test_load_ratio_overload(self):
        """超载情况"""
        ratio = ZoneLoadRatioCalculator.compute_load_ratio(1500, 1000)
        self.assertEqual(ratio, 1.5)

    def test_load_ratio_zero_capacity(self):
        """零承载力情况"""
        ratio = ZoneLoadRatioCalculator.compute_load_ratio(100, 0)
        self.assertEqual(ratio, float('inf'))


class TestNonlinearMappingFunction(unittest.TestCase):
    """测试非线性映射函数"""

    def setUp(self):
        self.func = NonlinearMappingFunction()

    def test_zero_load(self):
        """零负载"""
        self.assertAlmostEqual(self.func(0.0), 0.0, places=4)

    def test_first_segment(self):
        """第一段：线性增长"""
        ci = self.func(0.3)
        expected = 0.3 / 0.6 * 0.30
        self.assertAlmostEqual(ci, expected, places=4)

    def test_at_r1(self):
        """在r1处"""
        ci = self.func(0.6)
        self.assertAlmostEqual(ci, 0.30, places=4)

    def test_second_segment(self):
        """第二段：超线性增长"""
        ci = self.func(0.7)
        self.assertGreater(ci, 0.30)
        self.assertLess(ci, 0.70)

    def test_at_r2(self):
        """在r2处"""
        ci = self.func(0.8)
        self.assertAlmostEqual(ci, 0.70, places=4)

    def test_third_segment(self):
        """第三段：指数增长"""
        ci = self.func(1.0)
        self.assertGreater(ci, 0.70)
        self.assertLess(ci, 1.0)

    def test_saturation(self):
        """饱和区域"""
        ci = self.func(1.5)
        self.assertAlmostEqual(ci, 1.0, places=4)

    def test_negative_input(self):
        """负输入"""
        ci = self.func(-0.1)
        self.assertEqual(ci, 0.0)

    def test_derivative(self):
        """导数计算"""
        d1 = self.func.compute_derivative(0.3)
        self.assertAlmostEqual(d1, 0.5, places=4)

        d2 = self.func.compute_derivative(0.7)
        self.assertGreater(d2, d1)


class TestQueueWaitingCorrection(unittest.TestCase):
    """测试排队等待修正"""

    def setUp(self):
        self.func = QueueWaitingCorrection()

    def test_zero_wait(self):
        """零等待"""
        self.assertAlmostEqual(self.func(0.0), 0.0, places=4)

    def test_below_threshold(self):
        """阈值内等待"""
        ci = self.func(15.0)
        expected = 15.0 / 30.0 * 0.15
        self.assertAlmostEqual(ci, expected, places=4)

    def test_at_threshold(self):
        """在阈值处"""
        ci = self.func(30.0)
        self.assertAlmostEqual(ci, 0.15, places=4)

    def test_above_threshold(self):
        """超过阈值"""
        ci = self.func(60.0)
        self.assertGreater(ci, 0.15)

    def test_double_threshold(self):
        """双倍阈值时间"""
        ci_60 = self.func(60.0)
        ci_120 = self.func(120.0)
        self.assertGreater(ci_120, ci_60)

    def test_waiting_time_calculation(self):
        """等待时间计算"""
        wt = QueueWaitingCorrection.compute_waiting_time(
            queue_count=100,
            service_rate=2.0,
            service_windows=2
        )
        self.assertEqual(wt, 25.0)


class TestBottleneckAmplificationFactor(unittest.TestCase):
    """测试瓶颈放大因子"""

    def setUp(self):
        self.func = BottleneckAmplificationFactor()

    def test_no_bottleneck(self):
        """无瓶颈"""
        b = self.func(50, 100)
        self.assertEqual(b, 1.0)

    def test_at_capacity(self):
        """恰好达到容量"""
        b = self.func(100, 100)
        self.assertEqual(b, 1.0)

    def test_mild_bottleneck(self):
        """轻度瓶颈"""
        b = self.func(150, 100)
        expected = 1.5 ** 1.75
        self.assertAlmostEqual(b, expected, places=2)

    def test_severe_bottleneck(self):
        """严重瓶颈"""
        b = self.func(200, 100)
        expected = 2.0 ** 1.75
        self.assertAlmostEqual(b, expected, places=2)

    def test_zero_capacity(self):
        """零容量"""
        b = self.func(100, 0)
        self.assertEqual(b, float('inf'))


class TestCrowdingPropagation(unittest.TestCase):
    """测试拥挤传导"""

    def setUp(self):
        self.propagation = CrowdingPropagation()

    def test_no_propagation(self):
        """无传导"""
        result = self.propagation.compute({}, {})
        self.assertEqual(result, 0.0)

    def test_below_threshold(self):
        """低于阈值"""
        upstream = {"zone1": 0.3, "zone2": 0.4}
        coeffs = {"zone1": 0.1, "zone2": 0.1}
        result = self.propagation.compute(upstream, coeffs)
        self.assertEqual(result, 0.0)

    def test_above_threshold(self):
        """超过阈值"""
        upstream = {"zone1": 0.7, "zone2": 0.8}
        coeffs = {"zone1": 0.1, "zone2": 0.1}
        result = self.propagation.compute(upstream, coeffs)
        expected = 0.1 * (0.7 - 0.5) + 0.1 * (0.8 - 0.5)
        self.assertAlmostEqual(result, expected, places=4)


class TestZoneCrowdingIndexCalculator(unittest.TestCase):
    """测试区域拥挤指数计算"""

    def setUp(self):
        self.calculator = ZoneCrowdingIndexCalculator()

        self.zone = Zone(
            zone_id="core_1",
            name="核心景点",
            zone_type=ZoneType.CORE,
            area=1000.0,
            max_density=2.0,
            space_form=SpaceForm.OPEN
        )

    def test_low_crowding(self):
        """低拥挤情况"""
        state = ZoneState(
            zone_id="core_1",
            timestamp=0.0,
            visitor_count=500.0
        )

        result = self.calculator.compute(self.zone, state)

        self.assertLess(result.crowding_index, 0.3)
        self.assertEqual(result.crowding_level, CrowdingLevel.COMFORTABLE)

    def test_high_crowding(self):
        """高拥挤情况"""
        state = ZoneState(
            zone_id="core_1",
            timestamp=0.0,
            visitor_count=1800.0
        )

        result = self.calculator.compute(self.zone, state)

        self.assertGreater(result.crowding_index, 0.7)

    def test_with_bottleneck(self):
        """有瓶颈效应"""
        state = ZoneState(
            zone_id="core_1",
            timestamp=0.0,
            visitor_count=1500.0
        )

        result = self.calculator.compute(
            self.zone, state,
            outflow_demand=200,
            outflow_capacity=100
        )

        self.assertGreater(result.bottleneck_factor, 1.0)

    def test_service_zone_with_queue(self):
        """服务区域排队"""
        service_zone = Zone(
            zone_id="ride_1",
            name="游乐项目",
            zone_type=ZoneType.RIDE,
            area=200.0,
            max_density=2.0,
            service_rate=2.0,
            service_windows=2,
            space_form=SpaceForm.OPEN
        )

        state = ZoneState(
            zone_id="ride_1",
            timestamp=0.0,
            visitor_count=300.0,
            queue_count=120.0
        )

        result = self.calculator.compute(service_zone, state)

        self.assertGreater(result.queue_contribution, 0.0)


class TestScenicCrowdingAggregator(unittest.TestCase):
    """测试景区拥挤指数聚合"""

    def setUp(self):
        self.aggregator = ScenicCrowdingAggregator()

        self.zones = {
            "core_1": Zone(
                zone_id="core_1",
                name="核心景点",
                zone_type=ZoneType.CORE,
                area=1000.0,
                space_form=SpaceForm.OPEN
            ),
            "path_1": Zone(
                zone_id="path_1",
                name="主通道",
                zone_type=ZoneType.PATH,
                area=500.0,
                space_form=SpaceForm.OPEN
            )
        }

    def test_aggregation_basic(self):
        """基本聚合"""
        from core import ZoneCrowdingResult

        zone_results = {
            "core_1": ZoneCrowdingResult(
                zone_id="core_1",
                timestamp=0.0,
                visitor_count=1000,
                effective_capacity=1850,
                load_ratio=0.54,
                base_crowding=0.27,
                bottleneck_factor=1.0,
                queue_contribution=0.0,
                propagation_contribution=0.0,
                crowding_index=0.27,
                crowding_level=CrowdingLevel.COMFORTABLE
            ),
            "path_1": ZoneCrowdingResult(
                zone_id="path_1",
                timestamp=0.0,
                visitor_count=200,
                effective_capacity=925,
                load_ratio=0.22,
                base_crowding=0.11,
                bottleneck_factor=1.0,
                queue_contribution=0.0,
                propagation_contribution=0.0,
                crowding_index=0.11,
                crowding_level=CrowdingLevel.COMFORTABLE
            )
        }

        result = self.aggregator.aggregate(zone_results, self.zones)

        self.assertGreater(result.base_index, 0)
        self.assertLess(result.crowding_index, 0.5)

    def test_coupling_amplification(self):
        """耦合放大效应"""
        from core import ZoneCrowdingResult

        zone_results = {
            "core_1": ZoneCrowdingResult(
                zone_id="core_1",
                timestamp=0.0,
                visitor_count=1500,
                effective_capacity=1850,
                load_ratio=0.81,
                base_crowding=0.72,
                bottleneck_factor=1.0,
                queue_contribution=0.0,
                propagation_contribution=0.0,
                crowding_index=0.72,
                crowding_level=CrowdingLevel.VERY_CROWDED
            ),
            "path_1": ZoneCrowdingResult(
                zone_id="path_1",
                timestamp=0.0,
                visitor_count=600,
                effective_capacity=925,
                load_ratio=0.65,
                base_crowding=0.35,
                bottleneck_factor=1.0,
                queue_contribution=0.0,
                propagation_contribution=0.0,
                crowding_index=0.65,
                crowding_level=CrowdingLevel.CROWDED
            )
        }

        result = self.aggregator.aggregate(zone_results, self.zones)

        self.assertGreater(result.coupling_factor, 1.0)


class TestMultiScenicModel(unittest.TestCase):
    """测试多景区联动模型"""

    def setUp(self):
        self.transfer_model = TransferProbabilityModel()
        self.spillover_model = SpilloverModel()
        self.coordinator = MultiScenicCoordinator()

    def test_distance_calculation(self):
        """距离计算"""
        from multi_scenic import compute_distance

        loc_a = (116.4, 39.9)  # 北京
        loc_b = (121.5, 31.2)  # 上海

        dist = compute_distance(loc_a, loc_b)

        self.assertGreater(dist, 1000)
        self.assertLess(dist, 1500)

    def test_transfer_probability(self):
        """转移概率计算"""
        source = ScenicAttributes(
            scenic_id="A",
            name="景区A",
            attractiveness=80,
            total_capacity=10000,
            location=(116.4, 39.9)
        )

        targets = [
            ScenicAttributes(
                scenic_id="B",
                name="景区B",
                attractiveness=70,
                total_capacity=8000,
                location=(117.2, 39.1)  # 较近
            ),
            ScenicAttributes(
                scenic_id="C",
                name="景区C",
                attractiveness=90,
                total_capacity=12000,
                location=(118.0, 39.5)  # 较远
            )
        ]

        probs = self.transfer_model.compute_probabilities(
            source_attrs=source,
            target_attrs_list=targets,
            target_crowding_indices={"B": 0.3, "C": 0.6},
            target_remaining_capacities={"B": 5000, "C": 8000}
        )

        total_prob = sum(probs.values())
        self.assertAlmostEqual(total_prob, 1.0, places=4)

    def test_spillover_calculation(self):
        """溢出计算"""
        source = ScenicAttributes(
            scenic_id="A",
            name="景区A",
            attractiveness=80,
            total_capacity=10000,
            location=(116.4, 39.9)
        )

        targets = [
            ScenicAttributes(
                scenic_id="B",
                name="景区B",
                attractiveness=70,
                total_capacity=8000,
                location=(117.2, 39.1)
            )
        ]

        spillover = self.spillover_model.compute_spillover(
            source_attrs=source,
            source_visitor_count=12000,  # 超载
            target_attrs_list=targets,
            target_crowding_indices={"B": 0.3},
            target_remaining_capacities={"B": 5000}
        )

        self.assertGreater(spillover["B"], 0)

    def test_coordinator_state_matrix(self):
        """协调器状态矩阵"""
        from core import ScenicCrowdingResult

        attrs_a = ScenicAttributes(
            scenic_id="A",
            name="景区A",
            attractiveness=80,
            total_capacity=10000,
            location=(116.4, 39.9)
        )

        result_a = ScenicCrowdingResult(
            scenic_id="A",
            timestamp=0.0,
            crowding_index=0.75
        )

        self.coordinator.add_scenic_area(attrs_a, result_a, 8000)

        state = self.coordinator.get_state_matrix(0.0)

        self.assertIn("A", state.scenic_indices)
        self.assertEqual(state.scenic_indices["A"], 0.75)


class TestCrowdingIndexModel(unittest.TestCase):
    """测试完整模型"""

    def test_full_model_workflow(self):
        """完整工作流测试"""
        model = CrowdingIndexModel()

        scenic = ScenicArea(
            scenic_id="test_scenic",
            name="测试景区"
        )

        core_zone = Zone(
            zone_id="core_1",
            name="核心景点",
            zone_type=ZoneType.CORE,
            area=1000.0,
            max_density=2.0,
            space_form=SpaceForm.OPEN
        )

        entry_zone = Zone(
            zone_id="entry_1",
            name="入口",
            zone_type=ZoneType.ENTRY,
            area=200.0,
            max_density=2.0,
            service_rate=3.0,
            service_windows=4,
            space_form=SpaceForm.OPEN
        )

        scenic.add_zone(core_zone)
        scenic.add_zone(entry_zone)

        corridor = Corridor(
            corridor_id="c1",
            from_zone="entry_1",
            to_zone="core_1",
            length=100.0,
            width=3.0
        )
        scenic.add_corridor(corridor)

        zone_states = {
            "core_1": ZoneState(
                zone_id="core_1",
                timestamp=10.0,
                visitor_count=1500.0
            ),
            "entry_1": ZoneState(
                zone_id="entry_1",
                timestamp=10.0,
                visitor_count=300.0,
                queue_count=60.0
            )
        }

        result = model.compute_scenic_crowding(scenic, zone_states)

        self.assertIsNotNone(result)
        self.assertEqual(len(result.zone_results), 2)
        self.assertGreater(result.crowding_index, 0)


if __name__ == '__main__':
    unittest.main()
