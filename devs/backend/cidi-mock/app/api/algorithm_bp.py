from flask import Blueprint, request, jsonify
from typing import Dict, Any

from app.algorithm.kualk import (
    CrowdingIndexModel,
    ScenicArea,
    Zone,
    ZoneState,
    ZoneType,
    SpaceForm,
    MultiScenicCoordinator,
    ScenicAttributes,
    ScenicCrowdingResult
)

algorithm_bp = Blueprint('algorithm', __name__)

model = CrowdingIndexModel()


@algorithm_bp.route('/algorithm/crowd_index', methods=['POST'])
def crowd_index():
    """
    计算景区拥挤指数
    
    请求参数：
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
    
    响应：
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
    """
    try:
        data = request.get_json()
        
        scenic_id = data.get('scenic_id', 'scenic_001')
        scenic_name = data.get('scenic_name', '示例景区')
        
        scenic = ScenicArea(scenic_id=scenic_id, name=scenic_name)
        
        zones_data = data.get('zones', [])
        for zone_data in zones_data:
            zone = Zone(
                zone_id=zone_data.get('zone_id'),
                name=zone_data.get('name'),
                zone_type=ZoneType(zone_data.get('zone_type', 'CORE')),
                area=zone_data.get('area', 1000.0),
                max_density=zone_data.get('max_density', 2.5),
                service_rate=zone_data.get('service_rate', 0.0),
                service_windows=zone_data.get('service_windows', 0),
                space_form=SpaceForm(zone_data.get('space_form', 'open'))
            )
            scenic.add_zone(zone)
        
        zone_states_data = data.get('zone_states', {})
        zone_states = {}
        for zone_id, state_data in zone_states_data.items():
            state = ZoneState(
                zone_id=zone_id,
                timestamp=state_data.get('timestamp', 0.0),
                visitor_count=state_data.get('visitor_count', 0.0),
                queue_count=state_data.get('queue_count', 0.0),
                weather_factor=state_data.get('weather_factor', 1.0),
                event_factor=state_data.get('event_factor', 1.0),
                facility_factor=state_data.get('facility_factor', 1.0),
                light_factor=state_data.get('light_factor', 1.0)
            )
            zone_states[zone_id] = state
        
        result = model.compute_scenic_crowding(scenic, zone_states)
        
        return jsonify({
            'code': 200,
            'message': 'success',
            'data': {
                'scenic_id': result.scenic_id,
                'timestamp': result.timestamp,
                'crowding_index': result.crowding_index,
                'crowding_level': result.crowding_level.value,
                'base_index': result.base_index,
                'peak_index': result.peak_index,
                'coupling_factor': result.coupling_factor,
                'zone_results': {
                    zone_id: {
                        'zone_id': zone_result.zone_id,
                        'timestamp': zone_result.timestamp,
                        'visitor_count': zone_result.visitor_count,
                        'effective_capacity': zone_result.effective_capacity,
                        'load_ratio': zone_result.load_ratio,
                        'base_crowding': zone_result.base_crowding,
                        'bottleneck_factor': zone_result.bottleneck_factor,
                        'queue_contribution': zone_result.queue_contribution,
                        'propagation_contribution': zone_result.propagation_contribution,
                        'crowding_index': zone_result.crowding_index,
                        'crowding_level': zone_result.crowding_level.value
                    }
                    for zone_id, zone_result in result.zone_results.items()
                }
            }
        })
    
    except Exception as e:
        return jsonify({
            'code': 500,
            'message': f'Error: {str(e)}',
            'data': None
        }), 500


@algorithm_bp.route('/algorithm/multi_scenic', methods=['POST'])
def multi_scenic():
    """
    多景区联动计算
    
    请求参数：
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
    
    响应：
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
    """
    try:
        data = request.get_json()
        
        coordinator = MultiScenicCoordinator()
        
        scenic_areas_data = data.get('scenic_areas', {})
        for scenic_id, scenic_data in scenic_areas_data.items():
            attrs = ScenicAttributes(
                scenic_id=scenic_data.get('scenic_id'),
                name=scenic_data.get('name'),
                attractiveness=scenic_data.get('attractiveness', 50),
                total_capacity=scenic_data.get('total_capacity', 10000),
                location=tuple(scenic_data.get('location', [0.0, 0.0]))
            )
            coordinator.add_scenic_area(attrs)
        
        scenic_states_data = data.get('scenic_states', {})
        visitor_counts = data.get('visitor_counts', {})
        
        for scenic_id, state_data in scenic_states_data.items():
            result = ScenicCrowdingResult(
                scenic_id=state_data.get('scenic_id'),
                timestamp=state_data.get('timestamp', 0.0),
                crowding_index=state_data.get('crowding_index', 0.0)
            )
            coordinator.update_scenic_state(scenic_id, result, visitor_counts.get(scenic_id, 0))
        
        state_matrix = coordinator.get_state_matrix(0.0)
        recommendations = coordinator.get_recommendations()
        
        return jsonify({
            'code': 200,
            'message': 'success',
            'data': {
                'timestamp': state_matrix.timestamp,
                'scenic_indices': state_matrix.scenic_indices,
                'remaining_capacities': state_matrix.remaining_capacities,
                'spill_over_flows': {
                    f"{src}->{dst}": flow
                    for (src, dst), flow in state_matrix.spill_over_flows.items()
                },
                'recommendations': recommendations
            }
        })
    
    except Exception as e:
        return jsonify({
            'code': 500,
            'message': f'Error: {str(e)}',
            'data': None
        }), 500


@algorithm_bp.route('/algorithm/zone_crowding', methods=['POST'])
def zone_crowding():
    """
    计算单个区域的拥挤指数
    
    请求参数：
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
    
    响应：
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
    """
    try:
        data = request.get_json()
        
        zone_data = data.get('zone', {})
        zone = Zone(
            zone_id=zone_data.get('zone_id'),
            name=zone_data.get('name'),
            zone_type=ZoneType(zone_data.get('zone_type', 'CORE')),
            area=zone_data.get('area', 1000.0),
            max_density=zone_data.get('max_density', 2.5),
            service_rate=zone_data.get('service_rate', 0.0),
            service_windows=zone_data.get('service_windows', 0),
            space_form=SpaceForm(zone_data.get('space_form', 'open'))
        )
        
        state_data = data.get('state', {})
        state = ZoneState(
            zone_id=state_data.get('zone_id'),
            timestamp=state_data.get('timestamp', 0.0),
            visitor_count=state_data.get('visitor_count', 0.0),
            queue_count=state_data.get('queue_count', 0.0),
            weather_factor=state_data.get('weather_factor', 1.0),
            event_factor=state_data.get('event_factor', 1.0),
            facility_factor=state_data.get('facility_factor', 1.0),
            light_factor=state_data.get('light_factor', 1.0)
        )
        
        upstream_crowding = data.get('upstream_crowding', {})
        
        result = model.compute_zone_crowding(
            zone=zone,
            state=state,
            outflow_demand=0.0,
            outflow_capacity=float('inf'),
            upstream_crowding_indices=upstream_crowding
        )
        
        return jsonify({
            'code': 200,
            'message': 'success',
            'data': {
                'zone_id': result.zone_id,
                'timestamp': result.timestamp,
                'visitor_count': result.visitor_count,
                'effective_capacity': result.effective_capacity,
                'load_ratio': result.load_ratio,
                'base_crowding': result.base_crowding,
                'bottleneck_factor': result.bottleneck_factor,
                'queue_contribution': result.queue_contribution,
                'propagation_contribution': result.propagation_contribution,
                'crowding_index': result.crowding_index,
                'crowding_level': result.crowding_level.value
            }
        })
    
    except Exception as e:
        return jsonify({
            'code': 500,
            'message': f'Error: {str(e)}',
            'data': None
        }), 500
