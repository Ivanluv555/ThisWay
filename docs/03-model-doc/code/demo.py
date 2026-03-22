"""
景区游客拥挤指数数学模型 - 演示脚本

本脚本展示模型的实际计算效果，包括：
- 非线性映射函数曲线
- 不同场景下的拥挤指数计算
- 瓶颈效应演示
- 完整景区拥挤指数计算
- 多景区联动效果
"""

import sys
import os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from models import (
    Zone, ZoneType, SpaceForm, ZoneState,
    ScenicArea, Corridor, WeatherType,
    get_crowding_level, get_perception_weight,
    CrowdingLevel
)
from core import (
    MappingParams, NonlinearMappingFunction,
    QueueParams, QueueWaitingCorrection,
    BottleneckParams, BottleneckAmplificationFactor,
    CrowdingIndexModel
)
from multi_scenic import (
    ScenicAttributes, MultiScenicCoordinator
)


def print_separator(title: str):
    """打印分隔线"""
    print("\n" + "=" * 60)
    print(f"  {title}")
    print("=" * 60)


def demo_nonlinear_mapping():
    """演示非线性映射函数"""
    print_separator("1. 非线性映射函数 Φ(r) 演示")
    
    func = NonlinearMappingFunction()
    
    print("\n负载比 r → 拥挤指数 CI 的映射关系：")
    print("-" * 50)
    print(f"{'负载比 r':<15} {'拥挤指数 CI':<15} {'拥挤等级':<15}")
    print("-" * 50)
    
    test_ratios = [0.0, 0.2, 0.4, 0.5, 0.6, 0.65, 0.7, 0.75, 0.8, 0.85, 0.9, 1.0, 1.1, 1.2, 1.5]
    
    for r in test_ratios:
        ci = func(r)
        level = get_crowding_level(ci)
        level_names = {
            CrowdingLevel.COMFORTABLE: "I级-舒适",
            CrowdingLevel.MODERATE: "II级-一般",
            CrowdingLevel.CROWDED: "III级-拥挤",
            CrowdingLevel.VERY_CROWDED: "IV级-非常拥挤",
            CrowdingLevel.EXTREME: "V级-极度拥挤"
        }
        print(f"{r:<15.2f} {ci:<15.4f} {level_names[level]:<15}")
    
    print("\n关键特征说明：")
    print("  - r < 0.6 (60%负载)：线性缓慢增长，游客感觉舒适")
    print("  - 0.6 ≤ r < 0.8：超线性快速增长，拥挤感知加速上升")
    print("  - r ≥ 0.8：指数级跃升，接近承载极限时感知剧变")
    print("  - r ≥ 1.2：饱和，拥挤指数达到上限1.0")


def demo_queue_correction():
    """演示排队等待修正"""
    print_separator("2. 排队等待修正 Ψ(W) 演示")
    
    func = QueueWaitingCorrection()
    
    print("\n等待时间 W → 排队感知贡献 的映射关系：")
    print("-" * 50)
    print(f"{'等待时间(分钟)':<15} {'感知贡献':<15} {'说明':<20}")
    print("-" * 50)
    
    test_times = [0, 5, 10, 15, 20, 25, 30, 40, 50, 60, 90, 120, 180]
    
    for w in test_times:
        psi = func(w)
        if w == 0:
            desc = "无需等待"
        elif w < 30:
            desc = "阈值内，线性增长"
        elif w == 30:
            desc = "达到容忍阈值"
        else:
            desc = "超阈值，对数增长"
        print(f"{w:<15} {psi:<15.4f} {desc:<20}")
    
    print("\n关键特征说明：")
    print("  - W < 30分钟：线性增长，每分钟贡献 0.15/30 = 0.005")
    print("  - W = 30分钟：达到容忍阈值，贡献 0.15")
    print("  - W > 30分钟：对数增长，体现'每多等10分钟感知翻倍'规律")


def demo_bottleneck_effect():
    """演示瓶颈效应"""
    print_separator("3. 瓶颈效应放大因子 B 演示")
    
    func = BottleneckAmplificationFactor()
    
    print("\n出流需求/出流能力比 → 瓶颈放大因子：")
    print("-" * 50)
    print(f"{'需求/能力比':<15} {'放大因子 B':<15} {'说明':<20}")
    print("-" * 50)
    
    test_ratios = [0.5, 0.8, 1.0, 1.2, 1.5, 2.0, 2.5, 3.0]
    
    for ratio in test_ratios:
        demand = ratio * 100
        capacity = 100
        b = func(demand, capacity)
        if ratio <= 1.0:
            desc = "无瓶颈"
        else:
            desc = f"瓶颈！放大{b:.2f}倍"
        print(f"{ratio:<15.1f} {b:<15.4f} {desc:<20}")
    
    print("\n关键特征说明：")
    print("  - 需求 ≤ 能力：无瓶颈，B = 1.0")
    print("  - 需求 > 能力：瓶颈效应触发，拥挤指数被超线性放大")
    print("  - 例如：需求是能力的2倍时，B ≈ 3.36，拥挤指数放大3倍多")


def demo_zone_crowding():
    """演示区域拥挤指数计算"""
    print_separator("4. 区域拥挤指数计算演示")
    
    model = CrowdingIndexModel()
    
    core_zone = Zone(
        zone_id="core_1",
        name="核心景点-观景台",
        zone_type=ZoneType.CORE,
        area=500.0,
        max_density=3.0,
        space_form=SpaceForm.OPEN
    )
    
    print(f"\n区域信息：")
    print(f"  - 名称：{core_zone.name}")
    print(f"  - 类型：{core_zone.zone_type.value}")
    print(f"  - 面积：{core_zone.area} ㎡")
    print(f"  - 最大承载量：{core_zone.max_capacity} 人")
    print(f"  - 空间形态系数 κ：{core_zone.kappa:.3f}")
    print(f"  - 感知权重：{get_perception_weight(core_zone.zone_type):.2f}")
    
    print("\n不同客流下的拥挤指数：")
    print("-" * 70)
    print(f"{'游客数':<10} {'负载比':<10} {'基础拥挤':<12} {'瓶颈因子':<10} {'最终CI':<10} {'等级':<15}")
    print("-" * 70)
    
    visitor_counts = [200, 400, 600, 800, 1000, 1200, 1400, 1600, 1800, 2000]
    
    for count in visitor_counts:
        state = ZoneState(
            zone_id="core_1",
            timestamp=0.0,
            visitor_count=float(count)
        )
        
        result = model.compute_zone_crowding(core_zone, state)
        
        level_names = {
            CrowdingLevel.COMFORTABLE: "I级-舒适",
            CrowdingLevel.MODERATE: "II级-一般",
            CrowdingLevel.CROWDED: "III级-拥挤",
            CrowdingLevel.VERY_CROWDED: "IV级-非常拥挤",
            CrowdingLevel.EXTREME: "V级-极度拥挤"
        }
        
        print(f"{count:<10} {result.load_ratio:<10.2f} {result.base_crowding:<12.4f} "
              f"{result.bottleneck_factor:<10.2f} {result.crowding_index:<10.4f} "
              f"{level_names[result.crowding_level]:<15}")


def demo_service_zone():
    """演示服务区域（带排队）的拥挤指数"""
    print_separator("5. 服务区域排队拥挤演示")
    
    model = CrowdingIndexModel()
    
    ride_zone = Zone(
        zone_id="ride_1",
        name="网红游乐项目",
        zone_type=ZoneType.RIDE,
        area=200.0,
        max_density=2.0,
        service_rate=2.0,
        service_windows=2,
        space_form=SpaceForm.OPEN
    )
    
    print(f"\n服务区域信息：")
    print(f"  - 名称：{ride_zone.name}")
    print(f"  - 类型：{ride_zone.zone_type.value}")
    print(f"  - 服务窗口数：{ride_zone.service_windows}")
    print(f"  - 单窗口服务速率：{ride_zone.service_rate} 人/分钟")
    print(f"  - 总服务能力：{ride_zone.service_rate * ride_zone.service_windows} 人/分钟")
    
    print("\n不同排队人数下的拥挤指数（假设区域内固定200人）：")
    print("-" * 80)
    print(f"{'排队人数':<10} {'等待时间(分)':<12} {'排队贡献':<12} {'密度拥挤':<12} {'最终CI':<10} {'等级':<15}")
    print("-" * 80)
    
    queue_counts = [0, 20, 40, 60, 80, 100, 120, 150, 200, 300]
    
    for queue in queue_counts:
        state = ZoneState(
            zone_id="ride_1",
            timestamp=0.0,
            visitor_count=200.0,
            queue_count=float(queue)
        )
        
        result = model.compute_zone_crowding(ride_zone, state)
        
        waiting_time = queue / (ride_zone.service_rate * ride_zone.service_windows)
        
        level_names = {
            CrowdingLevel.COMFORTABLE: "I级-舒适",
            CrowdingLevel.MODERATE: "II级-一般",
            CrowdingLevel.CROWDED: "III级-拥挤",
            CrowdingLevel.VERY_CROWDED: "IV级-非常拥挤",
            CrowdingLevel.EXTREME: "V级-极度拥挤"
        }
        
        print(f"{queue:<10} {waiting_time:<12.1f} {result.queue_contribution:<12.4f} "
              f"{result.base_crowding:<12.4f} {result.crowding_index:<10.4f} "
              f"{level_names[result.crowding_level]:<15}")


def demo_full_scenic():
    """演示完整景区拥挤指数计算"""
    print_separator("6. 完整景区拥挤指数计算演示")
    
    scenic = ScenicArea(
        scenic_id="scenic_001",
        name="示例风景区"
    )
    
    zones = [
        Zone("entry_1", "入口检票区", ZoneType.ENTRY, 300.0, 2.0, service_rate=3.0, service_windows=4, space_form=SpaceForm.OPEN),
        Zone("core_1", "主峰观景台", ZoneType.CORE, 800.0, 2.5, space_form=SpaceForm.OPEN),
        Zone("core_2", "瀑布景区", ZoneType.CORE, 600.0, 2.5, space_form=SpaceForm.OPEN),
        Zone("ride_1", "玻璃栈道", ZoneType.RIDE, 200.0, 2.0, service_rate=1.5, service_windows=2, space_form=SpaceForm.SEMI_ENCLOSED),
        Zone("path_1", "主游览步道", ZoneType.PATH, 1000.0, 1.5, space_form=SpaceForm.OPEN),
        Zone("dining_1", "餐饮休息区", ZoneType.DINING, 400.0, 1.5, service_rate=2.0, service_windows=3, space_form=SpaceForm.SEMI_ENCLOSED),
        Zone("toilet_1", "卫生间A", ZoneType.TOILET, 80.0, 3.0, service_rate=3.0, service_windows=4, space_form=SpaceForm.ENCLOSED),
        Zone("shop_1", "文创商店", ZoneType.SHOP, 150.0, 2.0, space_form=SpaceForm.ENCLOSED),
        Zone("exit_1", "出口通道", ZoneType.EXIT, 200.0, 2.0, space_form=SpaceForm.OPEN),
    ]
    
    for zone in zones:
        scenic.add_zone(zone)
    
    print(f"\n景区：{scenic.name}")
    print(f"区域数量：{len(scenic.zones)}")
    
    print("\n区域配置信息：")
    print("-" * 90)
    print(f"{'区域ID':<10} {'名称':<15} {'类型':<10} {'面积(㎡)':<10} {'承载力':<10} {'权重':<8} {'形态系数':<10}")
    print("-" * 90)
    
    for zone in zones:
        print(f"{zone.zone_id:<10} {zone.name:<15} {zone.zone_type.value:<10} "
              f"{zone.area:<10.0f} {zone.max_capacity:<10.0f} "
              f"{get_perception_weight(zone.zone_type):<8.2f} {zone.kappa:<10.3f}")
    
    model = CrowdingIndexModel()
    
    print("\n" + "-" * 60)
    print("场景1：低客流（舒适状态）")
    print("-" * 60)
    
    zone_states_low = {
        "entry_1": ZoneState("entry_1", 0.0, 150.0, queue_count=20.0),
        "core_1": ZoneState("core_1", 0.0, 300.0),
        "core_2": ZoneState("core_2", 0.0, 200.0),
        "ride_1": ZoneState("ride_1", 0.0, 80.0, queue_count=30.0),
        "path_1": ZoneState("path_1", 0.0, 400.0),
        "dining_1": ZoneState("dining_1", 0.0, 100.0, queue_count=15.0),
        "toilet_1": ZoneState("toilet_1", 0.0, 30.0, queue_count=10.0),
        "shop_1": ZoneState("shop_1", 0.0, 40.0),
        "exit_1": ZoneState("exit_1", 0.0, 50.0),
    }
    
    result_low = model.compute_scenic_crowding(scenic, zone_states_low)
    
    print(f"\n景区整体拥挤指数：{result_low.crowding_index:.4f}")
    print(f"拥挤等级：{result_low.crowding_level.value}")
    print(f"基础指数：{result_low.base_index:.4f}")
    print(f"峰值指数：{result_low.peak_index:.4f}")
    print(f"耦合放大因子：{result_low.coupling_factor:.4f}")
    
    print("\n各区域拥挤指数：")
    for zone_id, zr in result_low.zone_results.items():
        zone = scenic.zones[zone_id]
        print(f"  {zone.name:<15} CI={zr.crowding_index:.4f}  负载比={zr.load_ratio:.2f}  "
              f"排队贡献={zr.queue_contribution:.4f}")
    
    print("\n" + "-" * 60)
    print("场景2：高客流（拥挤状态）")
    print("-" * 60)
    
    zone_states_high = {
        "entry_1": ZoneState("entry_1", 0.0, 450.0, queue_count=120.0),
        "core_1": ZoneState("core_1", 0.0, 1400.0),
        "core_2": ZoneState("core_2", 0.0, 1000.0),
        "ride_1": ZoneState("ride_1", 0.0, 250.0, queue_count=180.0),
        "path_1": ZoneState("path_1", 0.0, 1200.0),
        "dining_1": ZoneState("dining_1", 0.0, 350.0, queue_count=90.0),
        "toilet_1": ZoneState("toilet_1", 0.0, 80.0, queue_count=60.0),
        "shop_1": ZoneState("shop_1", 0.0, 120.0),
        "exit_1": ZoneState("exit_1", 0.0, 200.0),
    }
    
    result_high = model.compute_scenic_crowding(scenic, zone_states_high)
    
    print(f"\n景区整体拥挤指数：{result_high.crowding_index:.4f}")
    print(f"拥挤等级：{result_high.crowding_level.value}")
    print(f"基础指数：{result_high.base_index:.4f}")
    print(f"峰值指数：{result_high.peak_index:.4f}")
    print(f"耦合放大因子：{result_high.coupling_factor:.4f}")
    print(f"高负载区域数量：{result_high.high_load_zone_count}/{result_high.total_zones}")
    
    print("\n各区域拥挤指数：")
    for zone_id, zr in result_high.zone_results.items():
        zone = scenic.zones[zone_id]
        level_mark = "⚠️" if zr.crowding_index > 0.7 else ("⚡" if zr.crowding_index > 0.5 else "  ")
        print(f"  {level_mark} {zone.name:<15} CI={zr.crowding_index:.4f}  负载比={zr.load_ratio:.2f}  "
              f"排队贡献={zr.queue_contribution:.4f}")


def demo_weather_effect():
    """演示天气对拥挤指数的影响"""
    print_separator("7. 天气折减效应演示")
    
    model = CrowdingIndexModel()
    
    zone = Zone(
        zone_id="outdoor_1",
        name="室外广场",
        zone_type=ZoneType.PATH,
        area=1000.0,
        max_density=2.0,
        space_form=SpaceForm.OPEN
    )
    
    print(f"\n区域：{zone.name}")
    print(f"基础承载力：{zone.max_capacity:.0f} 人")
    print(f"空间形态系数：{zone.kappa:.3f}")
    
    print("\n不同天气下的有效承载力与拥挤指数（假设游客数800人）：")
    print("-" * 70)
    print(f"{'天气':<15} {'折减系数':<12} {'有效承载力':<12} {'负载比':<10} {'拥挤指数':<10}")
    print("-" * 70)
    
    weather_configs = [
        ("晴天/多云", 1.0),
        ("小雨/微风", 0.80),
        ("中雨/高温", 0.55),
        ("暴雨/极端", 0.35),
    ]
    
    for weather_name, weather_factor in weather_configs:
        state = ZoneState(
            zone_id="outdoor_1",
            timestamp=0.0,
            visitor_count=800.0,
            weather_factor=weather_factor
        )
        
        result = model.compute_zone_crowding(zone, state)
        
        print(f"{weather_name:<15} {weather_factor:<12.2f} {result.effective_capacity:<12.0f} "
              f"{result.load_ratio:<10.2f} {result.crowding_index:<10.4f}")
    
    print("\n说明：天气恶化时，室外区域有效承载力下降，相同客流下拥挤指数上升")


def demo_multi_scenic():
    """演示多景区联动"""
    print_separator("8. 多景区联动演示")
    
    coordinator = MultiScenicCoordinator()
    
    scenics = [
        ScenicAttributes("A", "热门景区A", attractiveness=90, total_capacity=10000, location=(116.4, 39.9)),
        ScenicAttributes("B", "次热门景区B", attractiveness=75, total_capacity=8000, location=(117.0, 39.5)),
        ScenicAttributes("C", "潜力景区C", attractiveness=60, total_capacity=6000, location=(116.8, 40.1)),
    ]
    
    for attrs in scenics:
        coordinator.add_scenic_area(attrs)
    
    print("\n景区配置：")
    print("-" * 70)
    print(f"{'景区ID':<10} {'名称':<15} {'吸引力':<10} {'承载力':<10} {'位置':<20}")
    print("-" * 70)
    for attrs in scenics:
        print(f"{attrs.scenic_id:<10} {attrs.name:<15} {attrs.attractiveness:<10} "
              f"{attrs.total_capacity:<10} {str(attrs.location):<20}")
    
    print("\n" + "-" * 60)
    print("场景：景区A超载，计算溢出效应")
    print("-" * 60)
    
    from core import ScenicCrowdingResult
    
    coordinator.update_scenic_state("A", ScenicCrowdingResult(scenic_id="A", timestamp=0.0, crowding_index=0.85), 12000)
    coordinator.update_scenic_state("B", ScenicCrowdingResult(scenic_id="B", timestamp=0.0, crowding_index=0.45), 4000)
    coordinator.update_scenic_state("C", ScenicCrowdingResult(scenic_id="C", timestamp=0.0, crowding_index=0.30), 2000)
    
    print("\n当前状态：")
    print(f"  景区A：游客数 12000人（超载2000人），拥挤指数 0.85")
    print(f"  景区B：游客数 4000人，拥挤指数 0.45")
    print(f"  景区C：游客数 2000人，拥挤指数 0.30")
    
    state_matrix = coordinator.get_state_matrix(0.0)
    
    print("\n多景区状态矩阵：")
    print(f"  各景区拥挤指数：{state_matrix.scenic_indices}")
    print(f"  各景区剩余容量：{state_matrix.remaining_capacities}")
    
    print("\n溢出客流计算：")
    for (src, dst), flow in state_matrix.spill_over_flows.items():
        if flow > 0:
            print(f"  {src} → {dst}：{flow:.1f} 人")
    
    recommendations = coordinator.get_recommendations()
    if recommendations:
        print("\n分流推荐：")
        for src, targets in recommendations.items():
            print(f"  景区{src} 建议分流至：{', '.join(targets)}")


def main():
    """主函数"""
    print("\n" + "█" * 60)
    print("█" + " " * 58 + "█")
    print("█" + "  景区游客拥挤指数数学模型 - 效果演示".center(50) + "  █")
    print("█" + " " * 58 + "█")
    print("█" * 60)
    
    demo_nonlinear_mapping()
    demo_queue_correction()
    demo_bottleneck_effect()
    demo_zone_crowding()
    demo_service_zone()
    demo_weather_effect()
    demo_full_scenic()
    demo_multi_scenic()
    
    print("\n" + "=" * 60)
    print("  演示完成")
    print("=" * 60 + "\n")


if __name__ == "__main__":
    main()
