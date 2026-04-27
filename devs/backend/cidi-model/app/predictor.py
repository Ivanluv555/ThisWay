"""
cidi-model — 核心预测引擎
原理来自 Future_Pridect.py，改造为接受结构化输入/输出，
不再直接读写 JSON 或数据库。
"""
import math
import random
from dataclasses import dataclass
from datetime import datetime, timedelta
from typing import Dict, List, Optional, Tuple

import numpy as np
import pandas as pd
import torch
import torch.nn as nn
from sklearn.preprocessing import MinMaxScaler
from torch.utils.data import DataLoader, Dataset

from app.models import (
    FlowBeforeAfter,
    HourlyFlowRecord,
    LocationInfo,
    PredictionRecord,
)

# ══════════════════════════════════════════════════════════════════
# 0. 工具函数
# ══════════════════════════════════════════════════════════════════
CHONGQING_CENTER = (29.5575, 106.5773)

PROFILE_LIBRARY = {
    "nightlife":       {"base_range": (12000, 25000), "peak_weights": (0.18, 0.48, 1.08), "weekend_boost": 0.30, "holiday_boost": 0.34, "night_event_boost": 0.42, "rain_coef": -0.11, "temp_coef": 0.04, "noise_std": 0.06, "spike_scale": 0.22},
    "commercial_core": {"base_range": (15000, 28000), "peak_weights": (0.24, 0.72, 0.92), "weekend_boost": 0.28, "holiday_boost": 0.33, "night_event_boost": 0.24, "rain_coef": -0.06, "temp_coef": 0.02, "noise_std": 0.06, "spike_scale": 0.18},
    "transport":       {"base_range": (8500,  16000), "peak_weights": (0.42, 0.82, 0.58), "weekend_boost": 0.26, "holiday_boost": 0.35, "night_event_boost": 0.16, "rain_coef": -0.13, "temp_coef": 0.03, "noise_std": 0.06, "spike_scale": 0.20},
    "historic":        {"base_range": (10000, 18000), "peak_weights": (0.26, 0.74, 0.68), "weekend_boost": 0.30, "holiday_boost": 0.36, "night_event_boost": 0.22, "rain_coef": -0.09, "temp_coef": 0.04, "noise_std": 0.06, "spike_scale": 0.20},
    "museum":          {"base_range": (6000,  12000), "peak_weights": (0.54, 0.80, 0.20), "weekend_boost": 0.19, "holiday_boost": 0.24, "night_event_boost": 0.06, "rain_coef":  0.03, "temp_coef":-0.01, "noise_std": 0.04, "spike_scale": 0.14},
    "park":            {"base_range": (5000,  10000), "peak_weights": (0.45, 0.64, 0.40), "weekend_boost": 0.28, "holiday_boost": 0.30, "night_event_boost": 0.10, "rain_coef": -0.18, "temp_coef": 0.06, "noise_std": 0.05, "spike_scale": 0.18},
    "theater":         {"base_range": (4500,   9500), "peak_weights": (0.08, 0.30, 1.00), "weekend_boost": 0.24, "holiday_boost": 0.30, "night_event_boost": 0.36, "rain_coef": -0.03, "temp_coef": 0.01, "noise_std": 0.05, "spike_scale": 0.22},
    "mountain_remote": {"base_range": (2000,   6000), "peak_weights": (0.42, 0.65, 0.22), "weekend_boost": 0.36, "holiday_boost": 0.38, "night_event_boost": 0.04, "rain_coef": -0.21, "temp_coef": 0.05, "noise_std": 0.07, "spike_scale": 0.20},
    "default":         {"base_range": (7000,  14000), "peak_weights": (0.30, 0.58, 0.36), "weekend_boost": 0.22, "holiday_boost": 0.28, "night_event_boost": 0.12, "rain_coef": -0.10, "temp_coef": 0.03, "noise_std": 0.05, "spike_scale": 0.16},
}

SPOT_PROFILE_OVERRIDES: Dict[str, Tuple[str, float]] = {
    "洪崖洞":        ("nightlife",       1.30), "解放碑":          ("commercial_core", 1.28),
    "长江索道":      ("transport",       1.22), "朝天门广场":      ("park",            1.08),
    "江北嘴公园":    ("park",            0.98), "观音桥":          ("commercial_core", 1.25),
    "大礼堂":        ("museum",          1.05), "十八梯":          ("historic",        1.15),
    "白公馆渣滓洞":  ("museum",          1.00), "歌乐山革命纪念馆": ("museum",          0.95),
    "南山植物园（北门）": ("park",            1.05), "南山植物园":      ("park",            1.05), "重庆大剧院":      ("theater",         1.08),
    "磁器口":        ("historic",        1.30), "建川博物馆":      ("museum",          0.92),
    "鸿恩寺":        ("park",            0.92), "鹅岭公园":        ("park",            0.95),
    "鹅岭印刷二厂":  ("historic",        1.02), "三峡博物馆":      ("museum",          1.18),
    "龙门浩":        ("nightlife",       1.05), "南滨路":          ("nightlife",       1.22),
    "金刚碑":        ("mountain_remote", 0.85),
}

PROFILE_CATEGORY_HINTS: Dict[str, Tuple[str, ...]] = {
    "nightlife": ("night", "bar", "酒吧", "夜", "夜游", "夜市"),
    "commercial_core": ("commercial", "mall", "shopping", "商圈", "商业", "步行街"),
    "transport": ("transport", "交通", "索道", "码头", "车站", "渡口"),
    "historic": ("historic", "history", "古", "历史", "文化街"),
    "museum": ("museum", "博物馆", "纪念馆", "展览"),
    "park": ("park", "公园", "园", "广场"),
    "theater": ("theater", "theatre", "剧院", "演艺"),
    "mountain_remote": ("mountain", "山", "郊野", "森林"),
}

# 仅对“有入园时段”的景区生效；商圈/夜生活默认全天候。
PROFILE_OPEN_WINDOWS: Dict[str, Optional[Tuple[float, float]]] = {
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

SPOT_OPEN_WINDOW_OVERRIDES: Dict[str, Tuple[float, float]] = {
    "长江索道": (8.0, 22.0),
    "三峡博物馆": (9.0, 17.0),
    "建川博物馆": (9.0, 17.0),
    "白公馆渣滓洞": (9.0, 17.0),
    "歌乐山革命纪念馆": (9.0, 17.0),
    "南山植物园（北门）": (8.0, 18.0),
    "南山植物园": (8.0, 18.0),
    "金刚碑": (8.0, 18.0),
}


def _set_seed(seed: int) -> None:
    random.seed(seed)
    np.random.seed(seed)
    torch.manual_seed(seed)


def _haversine_km(lat1, lon1, lat2, lon2) -> float:
    phi1, phi2 = math.radians(lat1), math.radians(lat2)
    d_phi = math.radians(lat2 - lat1)
    d_lam = math.radians(lon2 - lon1)
    a = math.sin(d_phi / 2) ** 2 + math.cos(phi1) * math.cos(phi2) * math.sin(d_lam / 2) ** 2
    return 6371.0 * (2 * math.atan2(math.sqrt(a), math.sqrt(1 - a)))


def _sigmoid(x: float) -> float:
    return 1.0 / (1.0 + math.exp(-x)) if x >= 0 else math.exp(x) / (1.0 + math.exp(x))


def _softmax(v: np.ndarray) -> np.ndarray:
    if v.size == 0:
        return v
    e = np.exp(v - np.max(v))
    s = float(np.sum(e))
    return e / s if s > 1e-12 else np.ones_like(v) / len(v)


def _round4(x: float) -> float:
    return float(round(float(x), 4))


def _gaussian(hours: np.ndarray, peak: float, width: float) -> np.ndarray:
    d = np.abs(hours - peak)
    d = np.minimum(d, 24 - d)
    return np.exp(-0.5 * (d / max(width, 1e-6)) ** 2)


def _parse_hour(value) -> Optional[float]:
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


def _infer_profile_key(title: str, category: Optional[str]) -> str:
    override = SPOT_PROFILE_OVERRIDES.get(title)
    if override:
        return override[0]
    if not category:
        return "default"
    text = str(category).lower()
    for profile_key, keywords in PROFILE_CATEGORY_HINTS.items():
        if any(k.lower() in text for k in keywords):
            return profile_key
    return "default"


def _resolve_open_window(loc: LocationInfo, profile_key: str) -> Optional[Tuple[float, float]]:
    for sk, ek in [
        ("entryStartHour", "entryEndHour"),
        ("openHour", "closeHour"),
        ("open_hour", "close_hour"),
        ("openTime", "closeTime"),
        ("open_time", "close_time"),
        ("startHour", "endHour"),
        ("start_hour", "end_hour"),
    ]:
        start = _parse_hour(getattr(loc, sk, None))
        end = _parse_hour(getattr(loc, ek, None))
        if start is not None and end is not None:
            return start, end

    if loc.title in SPOT_OPEN_WINDOW_OVERRIDES:
        return SPOT_OPEN_WINDOW_OVERRIDES[loc.title]
    return PROFILE_OPEN_WINDOWS.get(profile_key)


def _build_open_gate(hours: np.ndarray, window: Optional[Tuple[float, float]]) -> np.ndarray:
    if window is None:
        return np.ones_like(hours, dtype=float)
    start, end = window
    if abs(start - end) < 1e-6:
        return np.ones_like(hours, dtype=float)
    if start < end:
        return ((hours >= start) & (hours < end)).astype(float)
    return ((hours >= start) | (hours < end)).astype(float)


def _build_time_curve(hours: np.ndarray, profile_key: str) -> np.ndarray:
    if profile_key == "commercial_core":
        return (0.22
                + 0.95 * _gaussian(hours, 13.0, 2.3)
                + 1.05 * _gaussian(hours, 20.2, 2.6)
                + 0.25 * _gaussian(hours, 0.8, 2.2))
    if profile_key == "nightlife":
        return (0.14
                + 0.40 * _gaussian(hours, 13.5, 3.0)
                + 1.20 * _gaussian(hours, 21.3, 2.4)
                + 0.38 * _gaussian(hours, 0.7, 2.1))
    if profile_key == "park":
        # 公园类深夜接近 0，白天明显抬升。
        return (0.01
                + 0.96 * _gaussian(hours, 10.8, 2.5)
                + 0.74 * _gaussian(hours, 16.2, 2.7)
                + 0.05 * _gaussian(hours, 19.2, 1.8))
    if profile_key == "museum":
        return (0.05
                + 0.85 * _gaussian(hours, 10.3, 2.1)
                + 0.82 * _gaussian(hours, 14.2, 2.4)
                + 0.14 * _gaussian(hours, 18.0, 1.8))
    if profile_key == "historic":
        return (0.06
                + 0.68 * _gaussian(hours, 10.0, 2.4)
                + 0.92 * _gaussian(hours, 15.2, 2.7)
                + 0.30 * _gaussian(hours, 19.0, 2.3))
    if profile_key == "transport":
        return (0.12
                + 0.78 * _gaussian(hours, 9.2, 2.0)
                + 0.88 * _gaussian(hours, 15.8, 2.4)
                + 0.42 * _gaussian(hours, 19.2, 2.2))
    if profile_key == "mountain_remote":
        return (0.03
                + 0.88 * _gaussian(hours, 10.6, 2.6)
                + 0.64 * _gaussian(hours, 14.8, 2.9)
                + 0.10 * _gaussian(hours, 18.0, 2.0))
    if profile_key == "theater":
        return (0.08
                + 0.30 * _gaussian(hours, 14.0, 2.5)
                + 1.05 * _gaussian(hours, 20.5, 2.2)
                + 0.18 * _gaussian(hours, 23.0, 1.6))
    return (0.08
            + 0.82 * _gaussian(hours, 10.0, 2.5)
            + 0.96 * _gaussian(hours, 15.0, 2.8)
            + 0.32 * _gaussian(hours, 19.0, 2.2))


def _ar1_smooth(raw: np.ndarray, phi: float = 0.72) -> np.ndarray:
    out = np.zeros_like(raw)
    if len(raw) == 0:
        return out
    out[0] = raw[0]
    for i in range(1, len(raw)):
        out[i] = phi * out[i - 1] + (1 - phi) * raw[i]
    return out


# ══════════════════════════════════════════════════════════════════
# 1. 模拟历史人流（当数据库中历史数据不足时使用）
# ══════════════════════════════════════════════════════════════════
def generate_mock_history(
    locations: List[LocationInfo],
    hours: int = 168,
    seed: int = 42,
) -> pd.DataFrame:
    """生成模拟历史数据，返回 DataFrame（index=时间, columns=location_id）"""
    rng = np.random.default_rng(seed)
    time_index = pd.date_range(end=datetime.now().replace(minute=0, second=0, microsecond=0),
                               periods=hours, freq="h")
    hour_of_day = time_index.hour.values.astype(float)
    dow = time_index.dayofweek.values
    is_weekend = (dow >= 5).astype(float)
    is_fri_night = ((dow >= 4) & (hour_of_day >= 19) & (hour_of_day <= 23)).astype(float)

    days = int(math.ceil(hours / 24))
    rain_d = rng.beta(2.0, 5.0, days)
    temp_d = 14 + 7 * np.sin(np.linspace(0, 1.5 * np.pi, days)) + rng.normal(0, 1.8, days)
    day_idx = np.arange(hours) // 24
    rain = rain_d[day_idx]
    temp = temp_d[day_idx]

    city_shock = _ar1_smooth(rng.normal(0, 0.05, hours), phi=0.85) + \
                 0.03 * np.sin(np.linspace(0, 2 * np.pi, hours))

    unique_days = np.array(sorted({t.normalize() for t in time_index}))
    n_hol = max(1, int(len(unique_days) * 0.22))
    holiday_set = set(rng.choice(unique_days, size=n_hol, replace=False))
    is_holiday = np.array([1.0 if t.normalize() in holiday_set else 0.0 for t in time_index])

    data: Dict[int, np.ndarray] = {}
    for loc in locations:
        key = _infer_profile_key(loc.title, loc.category)
        hotspot = SPOT_PROFILE_OVERRIDES.get(loc.title, (key, 1.0))[1]
        prof = dict(PROFILE_LIBRARY[key])
        dist = _haversine_km(loc.latitude, loc.longitude, *CHONGQING_CENTER)
        urban = 0.72 + 0.35 * math.exp(-dist / 12.0)
        base = float(rng.uniform(*prof["base_range"]) * urban * hotspot)

        time_curve = _build_time_curve(hour_of_day, key)
        open_gate = _build_open_gate(hour_of_day, _resolve_open_window(loc, key))
        shape = time_curve * open_gate

        we = 1 + prof["weekend_boost"] * is_weekend
        he = 1 + prof["holiday_boost"] * is_holiday
        ne = 1 + prof["night_event_boost"] * is_fri_night
        wx = np.clip(1 + prof["rain_coef"] * rain + prof["temp_coef"] * ((temp - 18) / 10), 0.65, 1.35)

        spike = np.zeros(hours)
        for _ in range(max(1, hours // 72)):
            c = int(rng.integers(0, hours))
            w = float(rng.uniform(2.0, 5.5))
            spike += float(rng.uniform(0.05, prof["spike_scale"])) * np.exp(-0.5 * ((np.arange(hours) - c) / w) ** 2)

        noise = _ar1_smooth(rng.normal(0, prof["noise_std"], hours), phi=0.70)
        flow = base * shape * we * he * ne * wx * (1 + city_shock + noise + spike)
        flow = flow * open_gate
        data[loc.id] = np.clip(flow, 0.0, max(base * 4.5, 2000.0))

    return pd.DataFrame(data, index=time_index)


# ══════════════════════════════════════════════════════════════════
# 2. LSTM 模型
# ══════════════════════════════════════════════════════════════════
class _FlowDataset(Dataset):
    def __init__(self, data: np.ndarray, seq_len: int):
        self.t = torch.FloatTensor(data)
        self.s = seq_len

    def __len__(self):
        return len(self.t) - self.s

    def __getitem__(self, i):
        return self.t[i:i + self.s], self.t[i + self.s]


class _LSTMModel(nn.Module):
    def __init__(self, n: int, h: int = 64):
        super().__init__()
        self.lstm = nn.LSTM(input_size=n, hidden_size=h, batch_first=True)
        self.fc = nn.Linear(h, n)

    def forward(self, x):
        out, _ = self.lstm(x)
        return self.fc(out[:, -1, :])


def train_and_predict(
    df: pd.DataFrame,
    spot_ids: List[int],
    seed: int = 42,
    seq_len: int = 12,
    batch_size: int = 16,
    hidden_size: int = 64,
    epochs: int = 60,
) -> Dict[int, float]:
    """训练 LSTM 并返回下一小时各景点的预测人流量"""
    _set_seed(seed)
    scaler = MinMaxScaler()
    scaled = scaler.fit_transform(df.values)

    loader = DataLoader(_FlowDataset(scaled, seq_len), batch_size=batch_size, shuffle=True)
    model = _LSTMModel(len(spot_ids), hidden_size)
    opt = torch.optim.Adam(model.parameters(), lr=0.001)

    model.train()
    for _ in range(epochs):
        for bx, by in loader:
            opt.zero_grad()
            nn.MSELoss()(model(bx), by).backward()
            opt.step()

    model.eval()
    with torch.no_grad():
        inp = torch.FloatTensor(scaled[-seq_len:]).unsqueeze(0)
        raw = scaler.inverse_transform(model(inp).numpy())[0]

    return {spot_ids[i]: float(max(raw[i], 0.0)) for i in range(len(spot_ids))}


# ══════════════════════════════════════════════════════════════════
# 3. 分流策略（保留原始算法）
# ══════════════════════════════════════════════════════════════════
@dataclass
class DiversionPolicy:
    safe_ci: float = 5.0
    alpha: float = 1.0
    beta: float = 1.0
    distance_km_limit: float = 12.0
    similarity_scale_km: float = 5.0
    max_coupon: float = 80.0
    max_diversion_prob: float = 0.45
    theta0: float = -2.2; theta1: float = 0.05; theta2: float = 0.20
    theta3: float = 0.35; theta4: float = 1.10
    beta0: float = 0.00; beta1: float = 0.05; beta2: float = 0.22
    beta3: float = 0.25; beta4: float = 1.00; beta6: float = 0.80


def _build_dist_matrix(locations: List[LocationInfo]) -> Dict[int, Dict[int, float]]:
    by_id = {loc.id: loc for loc in locations}
    ids = [loc.id for loc in locations]
    m: Dict[int, Dict[int, float]] = {i: {} for i in ids}
    for i in ids:
        for j in ids:
            m[i][j] = 0.0 if i == j else _haversine_km(
                by_id[i].latitude, by_id[i].longitude,
                by_id[j].latitude, by_id[j].longitude)
    return m


def _ci(flow: float, cap: float, alpha: float, beta: float) -> float:
    return min(10.0, 10.0 * (flow / max(cap, 1e-6)) * alpha * beta)


def run_diversion_strategy(
    locations: List[LocationInfo],
    predicted_flow: Dict[int, float],
    capacity_map: Dict[int, float],
    policy: DiversionPolicy = DiversionPolicy(),
) -> Tuple[List[FlowBeforeAfter], list]:
    """运行非强制分流策略，返回 (flow_before_after, pair_recommendations)"""
    title = {loc.id: loc.title for loc in locations}
    dist = _build_dist_matrix(locations)
    spot_ids = list(predicted_flow.keys())

    safe_flow = {sid: capacity_map[sid] * (policy.safe_ci / 10.0) for sid in spot_ids}
    ci_before = {sid: _ci(predicted_flow[sid], capacity_map[sid], policy.alpha, policy.beta) for sid in spot_ids}
    flow_after = dict(predicted_flow)
    pairs = []

    for src in sorted(spot_ids, key=lambda s: predicted_flow[s] - safe_flow[s], reverse=True):
        overload = predicted_flow[src] - safe_flow[src]
        if overload <= 0:
            continue
        ol_ratio = min(1.0, overload / max(safe_flow[src], 1e-6))

        candidates = []
        for tgt in spot_ids:
            if tgt == src:
                continue
            d = dist[src][tgt]
            if d > policy.distance_km_limit:
                continue
            slack = max(safe_flow[tgt] - flow_after[tgt], 0.0)
            if slack <= 0:
                continue
            sr = min(1.0, slack / max(safe_flow[tgt], 1e-6))
            sim = math.exp(-d / max(policy.similarity_scale_km, 1e-6))
            coupon = policy.max_coupon * ol_ratio * sr * sim
            score  = 1.6 * sr + 1.2 * sim - 0.08 * d
            candidates.append({"tgt": tgt, "d": d, "sim": sim, "sr": sr, "coupon": coupon, "score": score})

        if not candidates:
            continue

        weights = _softmax(np.array([c["score"] for c in candidates], dtype=float))
        for i, c in enumerate(candidates):
            c["w"] = float(weights[i])

        x_bar = sum(c["coupon"] * c["w"] for c in candidates)
        d_bar = sum(c["d"] * c["w"] for c in candidates)
        ci_bar = sum(ci_before[c["tgt"]] * c["w"] for c in candidates)
        dci = max(ci_before[src] - ci_bar, 0.0)
        r_bar = sum(c["w"] ** 2 for c in candidates)

        ap = float(np.clip(
            _sigmoid(policy.theta0 + policy.theta1 * x_bar - policy.theta2 * d_bar
                     - policy.theta3 * dci + policy.theta4 * r_bar) * (0.7 + 0.6 * ol_ratio),
            0.0, policy.max_diversion_prob))

        utilities = [policy.beta0 + policy.beta1 * c["coupon"] - policy.beta2 * c["d"]
                     - policy.beta3 * ci_before[c["tgt"]] + policy.beta4 * c["sim"]
                     + policy.beta6 * c["w"] for c in candidates]
        alt_probs = _softmax(np.array(utilities, dtype=float))

        flow_after[src] -= predicted_flow[src] * ap
        for i, c in enumerate(candidates):
            fp = ap * float(alt_probs[i])
            tf = predicted_flow[src] * fp
            flow_after[c["tgt"]] += tf
            pairs.append({
                "sourceId": src, "sourceTitle": title.get(src, str(src)),
                "targetId": c["tgt"], "targetTitle": title.get(c["tgt"], str(c["tgt"])),
                "coupon": _round4(c["coupon"]), "finalProb": _round4(fp),
                "expectedTransferFlow": _round4(tf),
            })

    ci_after = {sid: _ci(flow_after[sid], capacity_map[sid], policy.alpha, policy.beta) for sid in spot_ids}
    fba = [
        FlowBeforeAfter(
            spotId=sid, spotTitle=title.get(sid, str(sid)),
            flowBefore=round(predicted_flow[sid], 2), flowAfter=round(flow_after[sid], 2),
            ciBefore=_round4(ci_before[sid]), ciAfter=_round4(ci_after[sid]),
        )
        for sid in spot_ids
    ]
    pairs.sort(key=lambda x: x["expectedTransferFlow"], reverse=True)
    return fba, pairs


# ══════════════════════════════════════════════════════════════════
# 4. 辅助：CI → 等级/趋势
# ══════════════════════════════════════════════════════════════════
def ci_to_level(ci: float) -> int:
    if ci <= 2:   return 1
    if ci <= 4:   return 2
    if ci <= 6:   return 3
    if ci <= 8:   return 4
    return 5


def flow_trend(predicted: float, history_mean: float) -> str:
    ratio = predicted / max(history_mean, 1.0)
    if ratio > 1.05:  return "上升"
    if ratio < 0.95:  return "下降"
    return "平稳"
