import requests
import json
from datetime import datetime


BASE_URL = "http://localhost:5000/api/v1"


def print_separator(title: str):
    print("\n" + "=" * 80)
    print(f"  {title}")
    print("=" * 80)


def test_crowd_index_chongqing():
    print_separator("测试1：重庆洪崖洞景区拥挤指数计算")
    
    request_data = {
        "scenic_id": "chongqing_hongyadong",
        "scenic_name": "重庆洪崖洞景区",
        "zones": [
            {
                "zone_id": "main_entrance",
                "name": "主入口检票区",
                "zone_type": "ENTRY",
                "area": 350.0,
                "max_density": 2.5,
                "service_rate": 4.0,
                "service_windows": 6,
                "space_form": "open"
            },
            {
                "zone_id": "main_building_1f",
                "name": "吊脚楼一层商业街",
                "zone_type": "CORE",
                "area": 1200.0,
                "max_density": 3.0,
                "space_form": "semi"
            },
            {
                "zone_id": "main_building_2f",
                "name": "吊脚楼二层观景台",
                "zone_type": "CORE",
                "area": 800.0,
                "max_density": 2.5,
                "space_form": "open"
            },
            {
                "zone_id": "night_view_platform",
                "name": "千厮门大桥观景台",
                "zone_type": "CORE",
                "area": 600.0,
                "max_density": 2.0,
                "space_form": "open"
            },
            {
                "zone_id": "photo_spot",
                "name": "网红打卡点",
                "zone_type": "RIDE",
                "area": 150.0,
                "max_density": 2.0,
                "service_rate": 1.5,
                "service_windows": 3,
                "space_form": "semi"
            },
            {
                "zone_id": "dining_area",
                "name": "特色餐饮区",
                "zone_type": "DINING",
                "area": 500.0,
                "max_density": 1.5,
                "service_rate": 2.0,
                "service_windows": 8,
                "space_form": "semi"
            },
            {
                "zone_id": "souvenir_shop",
                "name": "文创商店",
                "zone_type": "SHOP",
                "area": 300.0,
                "max_density": 2.0,
                "space_form": "enclosed"
            },
            {
                "zone_id": "walking_path",
                "name": "滨江步道",
                "zone_type": "PATH",
                "area": 2000.0,
                "max_density": 1.5,
                "space_form": "open"
            },
            {
                "zone_id": "toilet_main",
                "name": "主卫生间",
                "zone_type": "TOILET",
                "area": 100.0,
                "max_density": 3.0,
                "service_rate": 3.0,
                "service_windows": 6,
                "space_form": "enclosed"
            },
            {
                "zone_id": "exit_area",
                "name": "出口通道",
                "zone_type": "EXIT",
                "area": 250.0,
                "max_density": 2.0,
                "space_form": "open"
            }
        ],
        "zone_states": {
            "main_entrance": {
                "zone_id": "main_entrance",
                "timestamp": 0.0,
                "visitor_count": 520.0,
                "queue_count": 85.0,
                "weather_factor": 1.0,
                "event_factor": 1.2,
                "facility_factor": 1.0,
                "light_factor": 1.0
            },
            "main_building_1f": {
                "zone_id": "main_building_1f",
                "timestamp": 0.0,
                "visitor_count": 2100.0,
                "queue_count": 0.0,
                "weather_factor": 1.0,
                "event_factor": 1.2,
                "facility_factor": 1.0,
                "light_factor": 1.0
            },
            "main_building_2f": {
                "zone_id": "main_building_2f",
                "timestamp": 0.0,
                "visitor_count": 1450.0,
                "queue_count": 0.0,
                "weather_factor": 1.0,
                "event_factor": 1.2,
                "facility_factor": 1.0,
                "light_factor": 1.0
            },
            "night_view_platform": {
                "zone_id": "night_view_platform",
                "timestamp": 0.0,
                "visitor_count": 980.0,
                "queue_count": 0.0,
                "weather_factor": 1.0,
                "event_factor": 1.2,
                "facility_factor": 1.0,
                "light_factor": 1.0
            },
            "photo_spot": {
                "zone_id": "photo_spot",
                "timestamp": 0.0,
                "visitor_count": 220.0,
                "queue_count": 65.0,
                "weather_factor": 1.0,
                "event_factor": 1.2,
                "facility_factor": 1.0,
                "light_factor": 1.0
            },
            "dining_area": {
                "zone_id": "dining_area",
                "timestamp": 0.0,
                "visitor_count": 520.0,
                "queue_count": 45.0,
                "weather_factor": 1.0,
                "event_factor": 1.2,
                "facility_factor": 1.0,
                "light_factor": 1.0
            },
            "souvenir_shop": {
                "zone_id": "souvenir_shop",
                "timestamp": 0.0,
                "visitor_count": 380.0,
                "queue_count": 0.0,
                "weather_factor": 1.0,
                "event_factor": 1.2,
                "facility_factor": 1.0,
                "light_factor": 1.0
            },
            "walking_path": {
                "zone_id": "walking_path",
                "timestamp": 0.0,
                "visitor_count": 1800.0,
                "queue_count": 0.0,
                "weather_factor": 1.0,
                "event_factor": 1.2,
                "facility_factor": 1.0,
                "light_factor": 1.0
            },
            "toilet_main": {
                "zone_id": "toilet_main",
                "timestamp": 0.0,
                "visitor_count": 85.0,
                "queue_count": 35.0,
                "weather_factor": 1.0,
                "event_factor": 1.2,
                "facility_factor": 1.0,
                "light_factor": 1.0
            },
            "exit_area": {
                "zone_id": "exit_area",
                "timestamp": 0.0,
                "visitor_count": 280.0,
                "queue_count": 0.0,
                "weather_factor": 1.0,
                "event_factor": 1.2,
                "facility_factor": 1.0,
                "light_factor": 1.0
            }
        }
    }
    
    print("\n【请求参数】")
    print(f"景区：{request_data['scenic_name']}")
    print(f"区域数量：{len(request_data['zones'])}")
    print(f"总游客数：{sum(s['visitor_count'] for s in request_data['zone_states'].values()):.0f} 人")
    print(f"总排队数：{sum(s['queue_count'] for s in request_data['zone_states'].values()):.0f} 人")
    
    print("\n【发送请求】")
    print(f"URL: {BASE_URL}/algorithm/crowd_index")
    
    try:
        response = requests.post(
            f"{BASE_URL}/algorithm/crowd_index",
            json=request_data,
            headers={"Content-Type": "application/json"},
            timeout=10
        )
        
        print(f"\n【响应状态码】{response.status_code}")
        
        if response.status_code == 200:
            result = response.json()
            print("\n【响应结果】")
            print(json.dumps(result, ensure_ascii=False, indent=2))
            
            data = result.get('data', {})
            print(f"\n【关键指标】")
            print(f"景区拥挤指数：{data.get('crowding_index', 0):.4f}")
            print(f"拥挤等级：{data.get('crowding_level', 'N/A')}")
            print(f"基础指数：{data.get('base_index', 0):.4f}")
            print(f"峰值指数：{data.get('peak_index', 0):.4f}")
            print(f"耦合放大因子：{data.get('coupling_factor', 0):.4f}")
            
            print(f"\n【各区域拥挤情况】")
            zone_results = data.get('zone_results', {})
            for zone_id, zone_result in zone_results.items():
                zone_name = next((z['name'] for z in request_data['zones'] if z['zone_id'] == zone_id), zone_id)
                ci = zone_result.get('crowding_index', 0)
                level = zone_result.get('crowding_level', 'N/A')
                load_ratio = zone_result.get('load_ratio', 0)
                print(f"  {zone_name:<20} CI={ci:.4f}  等级={level:<12} 负载比={load_ratio:.2f}")
        else:
            print(f"\n【错误响应】")
            print(response.text)
    
    except requests.exceptions.ConnectionError:
        print("\n【连接失败】请确保Flask服务已启动（运行 python run.py）")
    except Exception as e:
        print(f"\n【请求异常】{str(e)}")


def test_zone_crowding_chongqing():
    print_separator("测试2：重庆解放碑步行街单个区域拥挤指数")
    
    request_data = {
        "zone": {
            "zone_id": "jiefangbei_main",
            "name": "解放碑步行街核心区",
            "zone_type": "CORE",
            "area": 3500.0,
            "max_density": 2.0,
            "space_form": "open"
        },
        "state": {
            "zone_id": "jiefangbei_main",
            "timestamp": 0.0,
            "visitor_count": 5200.0,
            "queue_count": 0.0,
            "weather_factor": 1.0,
            "event_factor": 1.1,
            "facility_factor": 1.0,
            "light_factor": 1.0
        },
        "upstream_crowding": {
            "metro_exit": 0.72,
            "shopping_mall": 0.65
        }
    }
    
    print("\n【请求参数】")
    zone = request_data['zone']
    state = request_data['state']
    print(f"区域：{zone['name']}")
    print(f"类型：{zone['zone_type']}")
    print(f"面积：{zone['area']} ㎡")
    print(f"最大承载力：{zone['area'] * zone['max_density']:.0f} 人")
    print(f"当前游客数：{state['visitor_count']:.0f} 人")
    print(f"上游拥挤度：{request_data['upstream_crowding']}")
    
    print("\n【发送请求】")
    print(f"URL: {BASE_URL}/algorithm/zone_crowding")
    
    try:
        response = requests.post(
            f"{BASE_URL}/algorithm/zone_crowding",
            json=request_data,
            headers={"Content-Type": "application/json"},
            timeout=10
        )
        
        print(f"\n【响应状态码】{response.status_code}")
        
        if response.status_code == 200:
            result = response.json()
            print("\n【响应结果】")
            print(json.dumps(result, ensure_ascii=False, indent=2))
            
            data = result.get('data', {})
            print(f"\n【关键指标】")
            print(f"拥挤指数：{data.get('crowding_index', 0):.4f}")
            print(f"拥挤等级：{data.get('crowding_level', 'N/A')}")
            print(f"有效承载力：{data.get('effective_capacity', 0):.0f} 人")
            print(f"负载比：{data.get('load_ratio', 0):.4f}")
            print(f"基础拥挤：{data.get('base_crowding', 0):.4f}")
            print(f"瓶颈因子：{data.get('bottleneck_factor', 0):.4f}")
            print(f"排队贡献：{data.get('queue_contribution', 0):.4f}")
            print(f"传播贡献：{data.get('propagation_contribution', 0):.4f}")
        else:
            print(f"\n【错误响应】")
            print(response.text)
    
    except requests.exceptions.ConnectionError:
        print("\n【连接失败】请确保Flask服务已启动（运行 python run.py）")
    except Exception as e:
        print(f"\n【请求异常】{str(e)}")


def test_multi_scenic_chongqing():
    print_separator("测试3：重庆多景区联动计算")
    
    request_data = {
        "scenic_areas": {
            "hongyadong": {
                "scenic_id": "hongyadong",
                "name": "洪崖洞",
                "attractiveness": 95,
                "total_capacity": 15000,
                "location": [106.58, 29.56]
            },
            "jiefangbei": {
                "scenic_id": "jiefangbei",
                "name": "解放碑步行街",
                "attractiveness": 90,
                "total_capacity": 20000,
                "location": [106.58, 29.55]
            },
            "chaotianmen": {
                "scenic_id": "chaotianmen",
                "name": "朝天门广场",
                "attractiveness": 85,
                "total_capacity": 12000,
                "location": [106.59, 29.57]
            },
            "cableway": {
                "scenic_id": "cableway",
                "name": "长江索道",
                "attractiveness": 88,
                "total_capacity": 3000,
                "location": [106.57, 29.55]
            },
            "ciqikou": {
                "scenic_id": "ciqikou",
                "name": "磁器口古镇",
                "attractiveness": 80,
                "total_capacity": 10000,
                "location": [106.45, 29.58]
            }
        },
        "scenic_states": {
            "hongyadong": {
                "scenic_id": "hongyadong",
                "timestamp": 0.0,
                "crowding_index": 0.88
            },
            "jiefangbei": {
                "scenic_id": "jiefangbei",
                "timestamp": 0.0,
                "crowding_index": 0.75
            },
            "chaotianmen": {
                "scenic_id": "chaotianmen",
                "timestamp": 0.0,
                "crowding_index": 0.62
            },
            "cableway": {
                "scenic_id": "cableway",
                "timestamp": 0.0,
                "crowding_index": 0.92
            },
            "ciqikou": {
                "scenic_id": "ciqikou",
                "timestamp": 0.0,
                "crowding_index": 0.45
            }
        },
        "visitor_counts": {
            "hongyadong": 18500,
            "jiefangbei": 16500,
            "chaotianmen": 9500,
            "cableway": 3800,
            "ciqikou": 5500
        }
    }
    
    print("\n【请求参数】")
    scenic_areas = request_data['scenic_areas']
    scenic_states = request_data['scenic_states']
    visitor_counts = request_data['visitor_counts']
    
    print("景区配置：")
    print(f"{'景区ID':<12} {'名称':<12} {'吸引力':<8} {'承载力':<10} {'游客数':<10} {'拥挤指数':<10}")
    print("-" * 70)
    for scenic_id, attrs in scenic_areas.items():
        visitors = visitor_counts.get(scenic_id, 0)
        ci = scenic_states.get(scenic_id, {}).get('crowding_index', 0)
        overload = "⚠️" if visitors > attrs['total_capacity'] else ""
        print(f"{scenic_id:<12} {attrs['name']:<12} {attrs['attractiveness']:<8} "
              f"{attrs['total_capacity']:<10} {visitors:<10} {ci:<10.2f} {overload}")
    
    print("\n【发送请求】")
    print(f"URL: {BASE_URL}/algorithm/multi_scenic")
    
    try:
        response = requests.post(
            f"{BASE_URL}/algorithm/multi_scenic",
            json=request_data,
            headers={"Content-Type": "application/json"},
            timeout=10
        )
        
        print(f"\n【响应状态码】{response.status_code}")
        
        if response.status_code == 200:
            result = response.json()
            print("\n【响应结果】")
            print(json.dumps(result, ensure_ascii=False, indent=2))
            
            data = result.get('data', {})
            print(f"\n【多景区状态矩阵】")
            print(f"各景区拥挤指数：{data.get('scenic_indices', {})}")
            print(f"各景区剩余容量：{data.get('remaining_capacities', {})}")
            
            print(f"\n【溢出客流】")
            spill_over = data.get('spill_over_flows', {})
            for route, flow in spill_over.items():
                if flow > 0:
                    print(f"  {route}：{flow:.1f} 人")
            
            print(f"\n【分流推荐】")
            recommendations = data.get('recommendations', {})
            for src, targets in recommendations.items():
                scenic_name = scenic_areas.get(src, {}).get('name', src)
                target_names = [scenic_areas.get(t, {}).get('name', t) for t in targets]
                print(f"  {scenic_name} → {', '.join(target_names)}")
        else:
            print(f"\n【错误响应】")
            print(response.text)
    
    except requests.exceptions.ConnectionError:
        print("\n【连接失败】请确保Flask服务已启动（运行 python run.py）")
    except Exception as e:
        print(f"\n【请求异常】{str(e)}")


def main():
    print("\n" + "█" * 80)
    print("█" + " " * 78 + "█")
    print("█" + "  重庆景区拥挤指数API测试".center(70) + "  █")
    print("█" + " " * 78 + "█")
    print("█" * 80)
    
    print(f"\n测试时间：{datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print(f"服务地址：{BASE_URL}")
    
    test_crowd_index_chongqing()
    test_zone_crowding_chongqing()
    test_multi_scenic_chongqing()
    
    print("\n" + "=" * 80)
    print("  测试完成")
    print("=" * 80 + "\n")


if __name__ == "__main__":
    main()
