import json
import math
import os
import random
from dataclasses import asdict, dataclass
from datetime import datetime
from typing import Dict, List, Optional, Tuple

import numpy as np
import pandas as pd
import torch
import torch.nn as nn
from sklearn.preprocessing import MinMaxScaler
from torch.utils.data import DataLoader, Dataset

# ==========================================
# 0. 辅助格式化函数
# ==========================================
def pad_zh(text: str, width: int) -> str:
    text = str(text)
    length = sum(2 if ord(c) > 127 else 1 for c in text)
    return text + ' ' * max(0, width - length)

# ==========================================
# 1. 配置与文件读取
# ==========================================
RANDOM_SEED = int(os.environ.get("SCENIC_SEED", "42"))

def resolve_json_file_path() -> str:
    env_path = os.environ.get("SCENIC_JSON_PATH")
    if env_path and os.path.exists(env_path):
        return env_path
    default_path = "D:/Study_resource/大创项目/code/Untitled-3.json"
    if os.path.exists(default_path):
        return default_path
    local_path = os.path.join(os.path.dirname(__file__), "Untitled-3.json")
    if os.path.exists(local_path):
        return local_path
    return default_path

JSON_FILE_PATH = resolve_json_file_path()

def set_random_seed(seed: int) -> None:
    random.seed(seed)
    np.random.seed(seed)
    torch.manual_seed(seed)

def load_scenic_spots(file_path: str) -> List[Dict]:
    if not os.path.exists(file_path):
        raise FileNotFoundError(f"错误：找不到文件 '{file_path}'。")
    with open(file_path, "r", encoding="utf-8") as f:
        data = json.load(f)
    print(f">>> 成功读取文件 '{file_path}'，共包含 {len(data)} 个景区节点。")
    return data

# ==========================================
# 2. 数据模拟 (修改版：史诗级放大真实客流量)
# ==========================================
CHONGQING_CENTER = (29.5575, 106.5773)

# 核心修改：将所有 base_range 放大 10~15倍，符合千万级人口大城市的吞吐量
PROFILE_LIBRARY = {
    "nightlife": {"base_range": (12000, 25000), "peak_weights": (0.18, 0.48, 1.08), "weekend_boost": 0.30, "holiday_boost": 0.34, "night_event_boost": 0.42, "rain_coef": -0.11, "temp_coef": 0.04, "noise_std": 0.06, "spike_scale": 0.22},
    "commercial_core": {"base_range": (15000, 28000), "peak_weights": (0.24, 0.72, 0.92), "weekend_boost": 0.28, "holiday_boost": 0.33, "night_event_boost": 0.24, "rain_coef": -0.06, "temp_coef": 0.02, "noise_std": 0.06, "spike_scale": 0.18},
    "transport": {"base_range": (8500, 16000), "peak_weights": (0.42, 0.82, 0.58), "weekend_boost": 0.26, "holiday_boost": 0.35, "night_event_boost": 0.16, "rain_coef": -0.13, "temp_coef": 0.03, "noise_std": 0.06, "spike_scale": 0.20},
    "historic": {"base_range": (10000, 18000), "peak_weights": (0.26, 0.74, 0.68), "weekend_boost": 0.30, "holiday_boost": 0.36, "night_event_boost": 0.22, "rain_coef": -0.09, "temp_coef": 0.04, "noise_std": 0.06, "spike_scale": 0.20},
    "museum": {"base_range": (6000, 12000), "peak_weights": (0.54, 0.80, 0.20), "weekend_boost": 0.19, "holiday_boost": 0.24, "night_event_boost": 0.06, "rain_coef": 0.03, "temp_coef": -0.01, "noise_std": 0.04, "spike_scale": 0.14},
    "park": {"base_range": (5000, 10000), "peak_weights": (0.45, 0.64, 0.40), "weekend_boost": 0.28, "holiday_boost": 0.30, "night_event_boost": 0.10, "rain_coef": -0.18, "temp_coef": 0.06, "noise_std": 0.05, "spike_scale": 0.18},
    "theater": {"base_range": (4500, 9500), "peak_weights": (0.08, 0.30, 1.00), "weekend_boost": 0.24, "holiday_boost": 0.30, "night_event_boost": 0.36, "rain_coef": -0.03, "temp_coef": 0.01, "noise_std": 0.05, "spike_scale": 0.22},
    "mountain_remote": {"base_range": (2000, 6000), "peak_weights": (0.42, 0.65, 0.22), "weekend_boost": 0.36, "holiday_boost": 0.38, "night_event_boost": 0.04, "rain_coef": -0.21, "temp_coef": 0.05, "noise_std": 0.07, "spike_scale": 0.20},
    "default": {"base_range": (7000, 14000), "peak_weights": (0.30, 0.58, 0.36), "weekend_boost": 0.22, "holiday_boost": 0.28, "night_event_boost": 0.12, "rain_coef": -0.10, "temp_coef": 0.03, "noise_std": 0.05, "spike_scale": 0.16},
}

SPOT_PROFILE_OVERRIDES = {
    "洪崖洞": ("nightlife", 1.30), "解放碑": ("commercial_core", 1.28), "长江索道": ("transport", 1.22), "朝天门广场": ("park", 1.08),
    "江北嘴公园": ("park", 0.98), "观音桥": ("commercial_core", 1.25), "大礼堂": ("museum", 1.05), "十八梯": ("historic", 1.15),
    "白公馆渣滓洞": ("museum", 1.00), "歌乐山革命纪念馆": ("museum", 0.95), "南山植物园（北门）": ("park", 1.05), "重庆大剧院": ("theater", 1.08),
    "磁器口": ("historic", 1.30), "建川博物馆": ("museum", 0.92), "鸿恩寺": ("park", 0.92), "鹅岭公园": ("park", 0.95),
    "鹅岭印刷二厂": ("historic", 1.02), "三峡博物馆": ("museum", 1.18), "龙门浩": ("nightlife", 1.05), "南滨路": ("nightlife", 1.22),
    "金刚碑": ("mountain_remote", 0.85),
}

CATEGORY_PROFILE_HINTS = {
    "nightlife": ("night", "bar", "酒吧", "夜", "夜游", "夜市"),
    "commercial_core": ("commercial", "mall", "shopping", "商圈", "商业", "步行街"),
    "transport": ("transport", "交通", "索道", "码头", "车站", "渡口"),
    "historic": ("historic", "history", "古", "历史", "文化街"),
    "museum": ("museum", "博物馆", "纪念馆", "展览"),
    "park": ("park", "公园", "园", "广场"),
    "theater": ("theater", "theatre", "剧院", "演艺"),
    "mountain_remote": ("mountain", "山", "郊野", "森林"),
}

# 仅在具备入园时段约束的景区上生效，商圈/夜游默认全天。
PROFILE_OPEN_WINDOWS = {
    "museum": (9.0, 17.0),
    "historic": (8.0, 21.0),
    "transport": (8.0, 22.0),
    "theater": (10.0, 22.5),
    "mountain_remote": (8.0, 18.0),
    "park": None,
    "nightlife": None,
    "commercial_core": None,
    "default": None,
}

SPOT_OPEN_WINDOW_OVERRIDES = {
    "长江索道": (8.0, 22.0),
    "三峡博物馆": (9.0, 17.0),
    "建川博物馆": (9.0, 17.0),
    "白公馆渣滓洞": (9.0, 17.0),
    "歌乐山革命纪念馆": (9.0, 17.0),
    "南山植物园（北门）": (8.0, 18.0),
    "南山植物园": (8.0, 18.0),
    "金刚碑": (8.0, 18.0),
}

def circular_hour_gaussian(hours: np.ndarray, peak: float, width: float) -> np.ndarray:
    distance = np.abs(hours - peak)
    distance = np.minimum(distance, 24 - distance)
    return np.exp(-0.5 * (distance / max(width, 1e-6)) ** 2)

def parse_hour_value(value) -> Optional[float]:
    if value is None:
        return None
    if isinstance(value, (int, float)):
        hour = float(value)
        return hour if 0.0 <= hour <= 24.0 else None
    if isinstance(value, str):
        text = value.strip()
        if not text:
            return None
        if ":" in text:
            hh, mm = text.split(":", 1)
            try:
                hour = float(int(hh)) + float(int(mm)) / 60.0
            except ValueError:
                return None
            return hour if 0.0 <= hour <= 24.0 else None
        try:
            hour = float(text)
            return hour if 0.0 <= hour <= 24.0 else None
        except ValueError:
            return None
    return None

def infer_profile_key(spot: Dict) -> str:
    title = str(spot.get("title", ""))
    override = SPOT_PROFILE_OVERRIDES.get(title)
    if override:
        return override[0]

    category = str(spot.get("category", "")).lower()
    if category:
        for profile_key, keywords in CATEGORY_PROFILE_HINTS.items():
            if any(k.lower() in category for k in keywords):
                return profile_key
    return "default"

def resolve_open_window(spot: Dict, profile_key: str) -> Optional[Tuple[float, float]]:
    for sk, ek in [
        ("entryStartHour", "entryEndHour"),
        ("openHour", "closeHour"),
        ("open_hour", "close_hour"),
        ("openTime", "closeTime"),
        ("open_time", "close_time"),
        ("startHour", "endHour"),
        ("start_hour", "end_hour"),
    ]:
        start = parse_hour_value(spot.get(sk))
        end = parse_hour_value(spot.get(ek))
        if start is not None and end is not None:
            return start, end

    title = str(spot.get("title", ""))
    if title in SPOT_OPEN_WINDOW_OVERRIDES:
        return SPOT_OPEN_WINDOW_OVERRIDES[title]
    return PROFILE_OPEN_WINDOWS.get(profile_key)

def build_open_gate(hours: np.ndarray, window: Optional[Tuple[float, float]]) -> np.ndarray:
    if window is None:
        return np.ones_like(hours, dtype=float)
    start, end = window
    if abs(start - end) < 1e-6:
        return np.ones_like(hours, dtype=float)
    if start < end:
        return ((hours >= start) & (hours < end)).astype(float)
    return ((hours >= start) | (hours < end)).astype(float)

def build_time_curve(hours: np.ndarray, profile_key: str) -> np.ndarray:
    if profile_key == "commercial_core":
        return (0.22
                + 0.95 * circular_hour_gaussian(hours, 13.0, 2.3)
                + 1.05 * circular_hour_gaussian(hours, 20.2, 2.6)
                + 0.25 * circular_hour_gaussian(hours, 0.8, 2.2))
    if profile_key == "nightlife":
        return (0.14
                + 0.40 * circular_hour_gaussian(hours, 13.5, 3.0)
                + 1.20 * circular_hour_gaussian(hours, 21.3, 2.4)
                + 0.38 * circular_hour_gaussian(hours, 0.7, 2.1))
    if profile_key == "park":
        # 公园类深夜近乎无人，白天活动集中。
        return (0.01
                + 0.96 * circular_hour_gaussian(hours, 10.8, 2.5)
                + 0.74 * circular_hour_gaussian(hours, 16.2, 2.7)
                + 0.05 * circular_hour_gaussian(hours, 19.2, 1.8))
    if profile_key == "museum":
        return (0.05
                + 0.85 * circular_hour_gaussian(hours, 10.3, 2.1)
                + 0.82 * circular_hour_gaussian(hours, 14.2, 2.4)
                + 0.14 * circular_hour_gaussian(hours, 18.0, 1.8))
    if profile_key == "historic":
        return (0.06
                + 0.68 * circular_hour_gaussian(hours, 10.0, 2.4)
                + 0.92 * circular_hour_gaussian(hours, 15.2, 2.7)
                + 0.30 * circular_hour_gaussian(hours, 19.0, 2.3))
    if profile_key == "transport":
        return (0.12
                + 0.78 * circular_hour_gaussian(hours, 9.2, 2.0)
                + 0.88 * circular_hour_gaussian(hours, 15.8, 2.4)
                + 0.42 * circular_hour_gaussian(hours, 19.2, 2.2))
    if profile_key == "mountain_remote":
        return (0.03
                + 0.88 * circular_hour_gaussian(hours, 10.6, 2.6)
                + 0.64 * circular_hour_gaussian(hours, 14.8, 2.9)
                + 0.10 * circular_hour_gaussian(hours, 18.0, 2.0))
    if profile_key == "theater":
        return (0.08
                + 0.30 * circular_hour_gaussian(hours, 14.0, 2.5)
                + 1.05 * circular_hour_gaussian(hours, 20.5, 2.2)
                + 0.18 * circular_hour_gaussian(hours, 23.0, 1.6))
    return (0.08
            + 0.82 * circular_hour_gaussian(hours, 10.0, 2.5)
            + 0.96 * circular_hour_gaussian(hours, 15.0, 2.8)
            + 0.32 * circular_hour_gaussian(hours, 19.0, 2.2))

def get_spot_profile(spot: Dict) -> Dict:
    title = str(spot.get("title", ""))
    profile_key = infer_profile_key(spot)
    hotspot = SPOT_PROFILE_OVERRIDES.get(title, (profile_key, 1.0))[1]
    profile = dict(PROFILE_LIBRARY[profile_key])
    profile["hotspot"] = hotspot
    profile["profile_key"] = profile_key
    return profile

def smooth_noise_ar1(raw_noise: np.ndarray, phi: float = 0.72) -> np.ndarray:
    smoothed = np.zeros_like(raw_noise)
    if len(raw_noise) == 0: return smoothed
    smoothed[0] = raw_noise[0]
    for idx in range(1, len(raw_noise)):
        smoothed[idx] = phi * smoothed[idx - 1] + (1 - phi) * raw_noise[idx]
    return smoothed

def simulate_city_weather(rng: np.random.Generator, hours: int) -> Dict[str, np.ndarray]:
    days = int(math.ceil(hours / 24))
    rain_daily = rng.beta(2.0, 5.0, days)
    temp_daily = 14 + 7 * np.sin(np.linspace(0, 1.5 * np.pi, days)) + rng.normal(0, 1.8, days)
    day_idx = np.arange(hours) // 24
    return {"rain": rain_daily[day_idx], "temp": temp_daily[day_idx]}

def build_city_shock(rng: np.random.Generator, hours: int) -> np.ndarray:
    base_noise = smooth_noise_ar1(rng.normal(0, 0.05, hours), phi=0.85)
    trend = 0.03 * np.sin(np.linspace(0, 2 * np.pi, hours))
    return base_noise + trend

def generate_mock_history_data(spots: List[Dict], hours: int = 168) -> pd.DataFrame:
    print(f">>> 正在基于重庆景区特征模拟 {hours} 小时历史客流...")
    rng = np.random.default_rng(RANDOM_SEED)

    data = {}
    time_index = pd.date_range(start="2025-12-01", periods=hours, freq="h")
    hour_of_day = time_index.hour.values.astype(float)
    day_of_week = time_index.dayofweek.values
    is_weekend = (day_of_week >= 5).astype(float)
    is_friday_night = ((day_of_week >= 4) & (hour_of_day >= 19) & (hour_of_day <= 23)).astype(float)

    weather = simulate_city_weather(rng, hours)
    city_shock = build_city_shock(rng, hours)

    unique_days = np.array(sorted({ts.normalize() for ts in time_index}))
    holiday_days_count = max(1, int(len(unique_days) * 0.22))
    holiday_days = set(rng.choice(unique_days, size=holiday_days_count, replace=False))
    is_holiday = np.array([1.0 if ts.normalize() in holiday_days else 0.0 for ts in time_index])

    for spot in spots:
        spot_id = int(spot["id"])
        profile = get_spot_profile(spot)
        lat = float(spot.get("latitude", CHONGQING_CENTER[0]))
        lon = float(spot.get("longitude", CHONGQING_CENTER[1]))
        dist_center = haversine_km(lat, lon, CHONGQING_CENTER[0], CHONGQING_CENTER[1])

        urban_factor = 0.72 + 0.35 * math.exp(-dist_center / 12.0)
        base_low, base_high = profile["base_range"]
        base_flow = float(rng.uniform(base_low, base_high) * urban_factor * profile["hotspot"])

        time_curve = build_time_curve(hour_of_day, profile["profile_key"])
        open_gate = build_open_gate(hour_of_day, resolve_open_window(spot, profile["profile_key"]))
        daily_shape = time_curve * open_gate

        weekend_effect = 1.0 + profile["weekend_boost"] * is_weekend
        holiday_effect = 1.0 + profile["holiday_boost"] * is_holiday
        night_event_effect = 1.0 + profile["night_event_boost"] * is_friday_night

        weather_effect = np.clip(1.0 + profile["rain_coef"] * weather["rain"] + profile["temp_coef"] * ((weather["temp"] - 18.0) / 10.0), 0.65, 1.35)

        local_spike = np.zeros(hours)
        for _ in range(max(1, hours // 72)):
            center = int(rng.integers(0, hours))
            local_spike += float(rng.uniform(0.05, profile["spike_scale"])) * np.exp(-0.5 * ((np.arange(hours) - center) / float(rng.uniform(2.0, 5.5))) ** 2)

        local_noise = smooth_noise_ar1(rng.normal(0, profile["noise_std"], hours), phi=0.70)
        flow = base_flow * daily_shape * weekend_effect * holiday_effect * night_event_effect * weather_effect * (1.0 + city_shock + local_noise + local_spike)
        flow = flow * open_gate
        data[spot_id] = np.clip(flow, 0.0, max(base_flow * 4.5, 2000.0))

    return pd.DataFrame(data, index=time_index)

# ==========================================
# 3. 数据集与模型
# ==========================================
class ScenicFlowDataset(Dataset):
    def __init__(self, data: np.ndarray, seq_len: int):
        self.data = torch.FloatTensor(data)
        self.seq_len = seq_len
    def __len__(self) -> int:
        return len(self.data) - self.seq_len
    def __getitem__(self, index: int):
        return self.data[index:index + self.seq_len], self.data[index + self.seq_len]

class LSTM_AR_Model(nn.Module):
    def __init__(self, num_nodes: int, hidden_size: int = 64):
        super().__init__()
        self.lstm = nn.LSTM(input_size=num_nodes, hidden_size=hidden_size, batch_first=True)
        self.fc = nn.Linear(hidden_size, num_nodes)
    def forward(self, x: torch.Tensor) -> torch.Tensor:
        lstm_out, _ = self.lstm(x)
        return self.fc(lstm_out[:, -1, :])


# ==========================================
# 4. 策略辅助函数 
# ==========================================
def sigmoid(x: float) -> float:
    return 1.0 / (1.0 + math.exp(-x)) if x >= 0 else math.exp(x) / (1.0 + math.exp(x))

def softmax(values: np.ndarray) -> np.ndarray:
    if values.size == 0: return values
    exp_values = np.exp(values - np.max(values))
    denom = float(np.sum(exp_values))
    return exp_values / denom if denom > 1e-12 else np.ones_like(values) / len(values)

def haversine_km(lat1: float, lon1: float, lat2: float, lon2: float) -> float:
    phi1, phi2 = math.radians(lat1), math.radians(lat2)
    d_phi, d_lambda = math.radians(lat2 - lat1), math.radians(lon2 - lon1)
    a = math.sin(d_phi / 2) ** 2 + math.cos(phi1) * math.cos(phi2) * math.sin(d_lambda / 2) ** 2
    return 6371.0 * (2 * math.atan2(math.sqrt(a), math.sqrt(1 - a)))

def build_distance_matrix(spots: List[Dict], spot_ids: List[int]) -> Dict[int, Dict[int, float]]:
    by_id = {int(s["id"]): s for s in spots}
    matrix: Dict[int, Dict[int, float]] = {sid: {} for sid in spot_ids}
    for i in spot_ids:
        for j in spot_ids:
            if i == j:
                matrix[i][j] = 0.0
            else:
                matrix[i][j] = haversine_km(
                    float(by_id[i].get("latitude", CHONGQING_CENTER[0])),
                    float(by_id[i].get("longitude", CHONGQING_CENTER[1])),
                    float(by_id[j].get("latitude", CHONGQING_CENTER[0])),
                    float(by_id[j].get("longitude", CHONGQING_CENTER[1]))
                )
    return matrix

def estimate_capacity_from_history(df_history: pd.DataFrame, spot_ids: List[int]) -> Dict[int, float]:
    return {sid: max(float(np.percentile(df_history[sid].values, 95)) * 1.15, 
                     float(np.mean(df_history[sid].values)) * 1.40, 3000.0) for sid in spot_ids}

def compute_ci_map(flow_map: Dict[int, float], capacity_map: Dict[int, float], alpha: float, beta: float) -> Dict[int, float]:
    return {sid: min(10.0, 10.0 * (flow / max(capacity_map[sid], 1e-6)) * alpha * beta) for sid, flow in flow_map.items()}

def as_number(value: float, ndigits: int = 4) -> float:
    return float(round(float(value), ndigits))

@dataclass
class DiversionPolicy:
    safe_ci: float = 5.0  
    alpha: float = 1.0; beta: float = 1.0
    distance_km_limit: float = 12.0; cluster_km: float = 6.0; similarity_scale_km: float = 5.0
    max_coupon: float = 80.0; max_diversion_prob: float = 0.45
    theta0: float = -2.2; theta1: float = 0.05; theta2: float = 0.20; theta3: float = 0.35; theta4: float = 1.10
    beta0: float = 0.00; beta1: float = 0.05; beta2: float = 0.22; beta3: float = 0.25; beta4: float = 1.00; beta5: float = 0.00; beta6: float = 0.80

# ==========================================
# 5. 非强制分流策略
# ==========================================
def build_non_forced_diversion_strategy(spots: List[Dict], spot_ids: List[int], predicted_flow: Dict[int, float], capacity_map: Dict[int, float], policy: DiversionPolicy) -> Dict:
    title_map = {int(s["id"]): str(s["title"]) for s in spots}
    distance_matrix = build_distance_matrix(spots, spot_ids)

    safe_flow = {sid: capacity_map[sid] * (policy.safe_ci / 10.0) for sid in spot_ids}
    ci_before = compute_ci_map(predicted_flow, capacity_map, policy.alpha, policy.beta)
    
    flow_after = {sid: float(predicted_flow[sid]) for sid in spot_ids}

    pair_recommendations = []
    source_acceptance = []

    source_order = sorted(spot_ids, key=lambda sid: predicted_flow[sid] - safe_flow[sid], reverse=True)

    for source_id in source_order:
        overload = predicted_flow[source_id] - safe_flow[source_id]
        if overload <= 0: continue 
        overload_ratio = min(1.0, overload / max(safe_flow[source_id], 1e-6))

        candidates = []
        for target_id in spot_ids:
            if target_id == source_id: continue
            distance_km = distance_matrix[source_id][target_id]
            if distance_km > policy.distance_km_limit: continue
            
            target_slack = max(safe_flow[target_id] - flow_after[target_id], 0.0)
            if target_slack <= 0.0: continue

            slack_ratio = min(1.0, target_slack / max(safe_flow[target_id], 1e-6))
            similarity = math.exp(-distance_km / max(policy.similarity_scale_km, 1e-6))

            coupon = policy.max_coupon * overload_ratio * slack_ratio * similarity
            score = 1.6 * slack_ratio + 1.2 * similarity - 0.08 * distance_km

            candidates.append({"target_id": target_id, "distance_km": distance_km, "similarity": similarity, "slack_ratio": slack_ratio, "coupon": coupon, "score": score})

        if not candidates: continue

        rec_weights = softmax(np.array([c["score"] for c in candidates], dtype=float))
        for idx, c in enumerate(candidates):
            c["recommend_weight"] = float(rec_weights[idx])

        x_bar = sum(c["coupon"] * c["recommend_weight"] for c in candidates)
        delta_d_bar = sum(c["distance_km"] * c["recommend_weight"] for c in candidates)
        ci_alt_bar = sum(ci_before[c["target_id"]] * c["recommend_weight"] for c in candidates)
        delta_ci_bar = max(ci_before[source_id] - ci_alt_bar, 0.0)
        r_bar = sum(c["recommend_weight"] ** 2 for c in candidates)

        accept_prob = np.clip(sigmoid(policy.theta0 + policy.theta1 * x_bar - policy.theta2 * delta_d_bar - policy.theta3 * delta_ci_bar + policy.theta4 * r_bar) * (0.7 + 0.6 * overload_ratio), 0.0, policy.max_diversion_prob)

        utilities = []
        for c in candidates:
            utility = policy.beta0 + policy.beta1 * c["coupon"] - policy.beta2 * c["distance_km"] - policy.beta3 * ci_before[c["target_id"]] + policy.beta4 * c["similarity"] + policy.beta6 * c["recommend_weight"]
            c["utility"] = float(utility)
            utilities.append(utility)

        alt_probs = softmax(np.array(utilities, dtype=float))
        transferred_total = predicted_flow[source_id] * float(accept_prob)
        flow_after[source_id] -= transferred_total

        source_acceptance.append({
            "source_id": source_id, "source_title": title_map.get(source_id, str(source_id)),
            "accept_prob": as_number(accept_prob), "expected_transfer_total": as_number(transferred_total, 2)
        })

        for idx, c in enumerate(candidates):
            final_prob = float(accept_prob) * float(alt_probs[idx])
            transfer_flow = predicted_flow[source_id] * final_prob
            flow_after[c["target_id"]] += transfer_flow

            pair_recommendations.append({
                "source_id": source_id, "source_title": title_map.get(source_id, str(source_id)),
                "target_id": c["target_id"], "target_title": title_map.get(c["target_id"], str(c["target_id"])),
                "coupon": as_number(c["coupon"], 2), "final_prob": as_number(final_prob),
                "expected_transfer_flow": as_number(transfer_flow, 2)
            })

    ci_after = compute_ci_map(flow_after, capacity_map, policy.alpha, policy.beta)

    flow_before_after = [
        {"spot_id": sid, "spot_title": title_map.get(sid, str(sid)), "flow_before": as_number(predicted_flow[sid], 2), "flow_after": as_number(flow_after[sid], 2), "ci_before": as_number(ci_before[sid]), "ci_after": as_number(ci_after[sid])}
        for sid in spot_ids
    ]

    pair_recommendations.sort(key=lambda x: x["expected_transfer_flow"], reverse=True)
    source_acceptance.sort(key=lambda x: x["expected_transfer_total"], reverse=True)

    summary = {
        "peak_ci_before": as_number(max(ci_before.values()) if ci_before else 0.0),
        "peak_ci_after": as_number(max(ci_after.values()) if ci_after else 0.0),
        "overloaded_spot_count_before": sum(1 for sid in spot_ids if ci_before[sid] > policy.safe_ci),
        "overloaded_spot_count_after": sum(1 for sid in spot_ids if ci_after[sid] > policy.safe_ci),
        "mean_accept_prob": as_number(float(np.mean([row["accept_prob"] for row in source_acceptance])) if source_acceptance else 0.0),
    }

    return {"flow_before_after": flow_before_after, "pair_recommendations": pair_recommendations, "summary": summary}


# ==========================================
# 6. 主程序逻辑
# ==========================================
def main() -> Dict:
    set_random_seed(RANDOM_SEED)

    try:
        spots = load_scenic_spots(JSON_FILE_PATH)
    except Exception as exc:
        print(f"数据加载失败，请检查 JSON_FILE_PATH ({JSON_FILE_PATH})。错误信息: {exc}")
        return {}

    spot_ids = [int(s["id"]) for s in spots]
    spot_titles = {int(s["id"]): str(s["title"]) for s in spots}

    df_history = generate_mock_history_data(spots)
    scaler = MinMaxScaler()
    data_scaled = scaler.fit_transform(df_history.values)

    seq_len, batch_size, hidden_size, epochs = 12, 16, 64, 60
    dataloader = DataLoader(ScenicFlowDataset(data_scaled, seq_len), batch_size=batch_size, shuffle=True)
    model = LSTM_AR_Model(len(spot_ids), hidden_size=hidden_size)
    optimizer = torch.optim.Adam(model.parameters(), lr=0.001)

    print(f">>> 开始训练模型...")
    model.train()
    for epoch in range(epochs):
        for batch_x, batch_y in dataloader:
            optimizer.zero_grad()
            loss = nn.MSELoss()(model(batch_x), batch_y)
            loss.backward()
            optimizer.step()

    print(">>> 正在生成下一小时预测...")
    model.eval()
    with torch.no_grad():
        pred_actual = scaler.inverse_transform(model(torch.FloatTensor(data_scaled[-seq_len:]).unsqueeze(0)).numpy())[0]
    
    predicted_flow = {spot_ids[idx]: float(max(pred_actual[idx], 0.0)) for idx in range(len(spot_ids))}

    policy = DiversionPolicy() 
    strategy = build_non_forced_diversion_strategy(spots, spot_ids, predicted_flow, estimate_capacity_from_history(df_history, spot_ids), policy)

    print("\n" + "=" * 60)
    print("      下一小时景区客流预测 (Top 10)      ")
    print("=" * 60)
    for sid, flow in sorted(predicted_flow.items(), key=lambda x: x[1], reverse=True)[:10]:
        print(f"{pad_zh(spot_titles.get(sid, str(sid)), 16)} | 估计值 | {int(flow):>7} 人")

    print("\n" + "=" * 80)
    print("      非强制分流推荐 (Top 10)      ")
    print("=" * 80)
    if not strategy["pair_recommendations"]:
        print("当前没有任何景区超过安全阈值 (CI > 5.0)，无需触发分流方案。")
    else:
        for row in strategy["pair_recommendations"][:10]:
            print(f"{pad_zh(row['source_title'], 14)} -> {pad_zh(row['target_title'], 14)} | 券:{row['coupon']:>4.1f} | 最终概率:{row['final_prob']:.3f} | 预计引导:{row['expected_transfer_flow']:>7.1f} 人")

    print("\n" + "=" * 90)
    print("      本来的人流状况  vs  经过分流后的人流情况      ")
    print("=" * 90)
    print(f"{pad_zh('景区名称', 16)} | {'原本人流':>10} | {'分流后人流':>10} | {'变化量':>10} | {'CI变化'}")
    print("-" * 90)
    for row in sorted(strategy["flow_before_after"], key=lambda r: r["flow_before"], reverse=True):
        delta_flow = row["flow_after"] - row["flow_before"]
        delta_str = f"+{delta_flow:.1f}" if delta_flow > 0 else (f"{delta_flow:.1f}" if delta_flow < 0 else "  0.0")
        print(f"{pad_zh(row['spot_title'], 16)} | {row['flow_before']:>10.1f} | {row['flow_after']:>10.1f} | {delta_str:>10} | {row['ci_before']:.2f}->{row['ci_after']:.2f}")

    return strategy

if __name__ == "__main__":
    main()