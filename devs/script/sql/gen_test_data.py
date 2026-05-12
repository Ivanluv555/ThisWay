"""
Generate INSERT test data for locations_predictions and location_transitions.
Uses the same formulas as predictor.py for realistic values.
"""
import math
from datetime import datetime, timedelta

# ═══════════════════════════════════════════════════════════
# Profile config (same as predictor.py)
# ═══════════════════════════════════════════════════════════

def _circular_hour_gaussian(hour, peak, width=2.5):
    d = abs(hour - peak)
    d = min(d, 24 - d)
    return math.exp(-0.5 * (d / max(width, 1e-6)) ** 2)

def build_time_curve(hour, profile_key):
    if profile_key == "commercial_core":
        return (0.22 + 0.95 * _circular_hour_gaussian(hour, 13.0, 2.3)
                + 1.05 * _circular_hour_gaussian(hour, 20.2, 2.6)
                + 0.25 * _circular_hour_gaussian(hour, 0.8, 2.2))
    if profile_key == "nightlife":
        return (0.14 + 0.40 * _circular_hour_gaussian(hour, 13.5, 3.0)
                + 1.20 * _circular_hour_gaussian(hour, 21.3, 2.4)
                + 0.38 * _circular_hour_gaussian(hour, 0.7, 2.1))
    if profile_key == "park":
        return (0.01 + 0.96 * _circular_hour_gaussian(hour, 10.8, 2.5)
                + 0.74 * _circular_hour_gaussian(hour, 16.2, 2.7)
                + 0.05 * _circular_hour_gaussian(hour, 19.2, 1.8))
    if profile_key == "museum":
        return (0.05 + 0.85 * _circular_hour_gaussian(hour, 10.3, 2.1)
                + 0.82 * _circular_hour_gaussian(hour, 14.2, 2.4)
                + 0.14 * _circular_hour_gaussian(hour, 18.0, 1.8))
    if profile_key == "historic":
        return (0.06 + 0.68 * _circular_hour_gaussian(hour, 10.0, 2.4)
                + 0.92 * _circular_hour_gaussian(hour, 15.2, 2.7)
                + 0.30 * _circular_hour_gaussian(hour, 19.0, 2.3))
    if profile_key == "transport":
        return (0.12 + 0.78 * _circular_hour_gaussian(hour, 9.2, 2.0)
                + 0.88 * _circular_hour_gaussian(hour, 15.8, 2.4)
                + 0.42 * _circular_hour_gaussian(hour, 19.2, 2.2))
    if profile_key == "mountain_remote":
        return (0.03 + 0.88 * _circular_hour_gaussian(hour, 10.6, 2.6)
                + 0.64 * _circular_hour_gaussian(hour, 14.8, 2.9)
                + 0.10 * _circular_hour_gaussian(hour, 18.0, 2.0))
    if profile_key == "theater":
        return (0.08 + 0.30 * _circular_hour_gaussian(hour, 14.0, 2.5)
                + 1.05 * _circular_hour_gaussian(hour, 20.5, 2.2)
                + 0.18 * _circular_hour_gaussian(hour, 23.0, 1.6))
    return (0.08 + 0.82 * _circular_hour_gaussian(hour, 10.0, 2.5)
            + 0.96 * _circular_hour_gaussian(hour, 15.0, 2.8)
            + 0.32 * _circular_hour_gaussian(hour, 19.0, 2.2))

OPEN_WINDOWS = {
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

def build_open_gate(hour, window):
    if window is None:
        return 1.0
    start, end = window
    if abs(start - end) < 1e-6:
        return 1.0
    if start < end:
        return 1.0 if start <= hour < end else 0.0
    return 1.0 if (hour >= start or hour < end) else 0.0

# ═══════════════════════════════════════════════════════════
# Location definitions (IDs 1-21)
# ═══════════════════════════════════════════════════════════
# Profile, base_flow (daily peak), capacity
locations = {
    1:  {"profile": "nightlife",       "base": 18000, "cap": 22000},
    2:  {"profile": "commercial_core", "base": 22000, "cap": 26000},
    3:  {"profile": "transport",       "base": 12000, "cap": 15000},
    4:  {"profile": "park",            "base": 7500,  "cap": 10000},
    5:  {"profile": "park",            "base": 6500,  "cap": 9000},
    6:  {"profile": "commercial_core", "base": 20000, "cap": 24000},
    7:  {"profile": "museum",          "base": 9000,  "cap": 11000},
    8:  {"profile": "historic",        "base": 14000, "cap": 17000},
    9:  {"profile": "museum",          "base": 8000,  "cap": 10000},
    10: {"profile": "museum",          "base": 7500,  "cap": 9500},
    11: {"profile": "park",            "base": 8000,  "cap": 10500},
    12: {"profile": "park",            "base": 7000,  "cap": 9500},
    13: {"profile": "theater",         "base": 7000,  "cap": 9000},
    14: {"profile": "historic",        "base": 16000, "cap": 19000},
    15: {"profile": "museum",          "base": 7000,  "cap": 9000},
    16: {"profile": "park",            "base": 5500,  "cap": 8000},
    17: {"profile": "park",            "base": 6000,  "cap": 8500},
    18: {"profile": "historic",        "base": 11000, "cap": 14000},
    19: {"profile": "museum",          "base": 10000, "cap": 12500},
    20: {"profile": "nightlife",       "base": 14000, "cap": 17000},
    21: {"profile": "nightlife",       "base": 22000, "cap": 26000},
}

def ci_to_level(ci):
    if ci <= 2:   return 0  # 舒适
    if ci <= 5:   return 1  # 适中
    if ci <= 7:   return 2  # 拥挤
    return 3               # 爆满

def traffic_level_from_ci(ci):
    if ci <= 2:   return 1
    if ci <= 4:   return 2
    if ci <= 6:   return 3
    if ci <= 8:   return 4
    return 5

# ═══════════════════════════════════════════════════════════
# Generate predictions
# ═══════════════════════════════════════════════════════════
base_date = "2026-05-13"
hours_list = list(range(0, 24, 2))  # 00, 02, 04, ..., 22

predictions = []  # list of tuples

for loc_id, cfg in locations.items():
    profile = cfg["profile"]
    base = cfg["base"]
    cap = cfg["cap"]
    window = OPEN_WINDOWS.get(profile)

    prev_flow = None
    for h in hours_list:
        tc = build_time_curve(h, profile)
        gate = build_open_gate(h, window)

        # Add some variation: ±10-15% noise, weekend boost for Saturday
        weekend_mult = 1.18 if base_date == "2026-05-13" else 1.0  # May 13 2026 is a Wednesday
        # Actually let's check: 2026-05-13... Jan 1 2026 is Thursday.
        # Jan has 31 days, Feb 28, Mar 31, Apr 30 = 120 days
        # 120 + 12 (May 1-12) = 132 days from Jan 1
        # Jan 1 = Thu (day 3 in weekday()), so May 13 weekday = (3 + 132) % 7 = 135 % 7 = 2 = Tuesday
        # Actually weekday(): Monday=0, Tuesday=1, Wednesday=2, Thursday=3, Friday=4, Saturday=5, Sunday=6
        # Jan 1 2026 is Thursday (3). May 13 = Jan 1 + 132 days. 132 % 7 = 6. 3+6=9. 9%7=2. Wednesday.
        # So it's a weekday. No weekend boost.
        weekday_mult = 1.0

        flow = base * tc * gate * weekday_mult
        flow = max(0, flow)

        # Add small random variation per hour (deterministic via simple formula)
        variation = 1.0 + 0.06 * math.sin(loc_id * 1.7 + h * 0.9) + 0.04 * math.cos(loc_id * 0.6 + h * 1.3)
        flow = flow * variation

        visitor_count = int(round(flow))
        if visitor_count < 50:
            visitor_count = int(50 + 30 * math.sin(loc_id + h))  # floor for night hours

        ci = min(10.0, 10.0 * flow / max(cap, 1e-6))
        congestion_level = ci_to_level(ci)
        crowd_density = round(ci, 2)
        traffic_level = traffic_level_from_ci(ci)

        # Determine trend
        if prev_flow is not None and prev_flow > 100:
            ratio = flow / max(prev_flow, 1.0)
            if ratio > 1.08:
                trend = "上升"
            elif ratio < 0.92:
                trend = "下降"
            else:
                trend = "平稳"
        else:
            trend = "平稳"

        predict_time = f"{base_date} {h:02d}:00:00"
        create_time_dt = datetime(2026, 5, 13, h) - timedelta(hours=1)
        if create_time_dt.day < 13:
            create_time = f"2026-05-12 {create_time_dt.hour:02d}:00:00"
        else:
            create_time = f"2026-05-13 {create_time_dt.hour:02d}:00:00"

        predictions.append((loc_id, predict_time, visitor_count, congestion_level,
                           trend, create_time, crowd_density, traffic_level))
        prev_flow = flow

# ═══════════════════════════════════════════════════════════
# Generate transitions (diversion pairs)
# ═══════════════════════════════════════════════════════════
# For each hour, find overloaded sources and generate diversion targets
transitions = []

# Define "adjacency" groups for realistic diversions
diversion_groups = [
    [1, 20, 21],        # nightlife cluster
    [2, 6],              # commercial cluster
    [4, 5, 11, 12, 16, 17],  # parks cluster
    [7, 9, 10, 15, 19],      # museums cluster
    [8, 14, 18],             # historic cluster
    [13, 3],                 # theater + transport (nearby)
]

for h in hours_list:
    # Get congestion levels for this hour
    hour_preds = {p[0]: p for p in predictions if p[1] == f"{base_date} {h:02d}:00:00"}

    overloaded = [lid for lid, p in hour_preds.items() if p[3] >= 2]  # congestion_level >= 2
    comfortable = [lid for lid, p in hour_preds.items() if p[3] <= 1]  # congestion_level <= 1

    if len(overloaded) < 1 or len(comfortable) < 2:
        continue

    # For each overloaded source, find targets in same group with slack
    for src_id in overloaded[:3]:  # max 3 overloaded sources per hour
        src_flow = hour_preds[src_id][2]
        src_group = None
        for g in diversion_groups:
            if src_id in g:
                src_group = g
                break

        # Find comfortable targets (prefer same group, then others)
        same_group_targets = [t for t in comfortable if t != src_id and src_group and t in src_group]
        other_targets = [t for t in comfortable if t != src_id and t not in same_group_targets]

        # Pick 2-4 targets
        targets = same_group_targets[:2] + other_targets[:2]
        targets = targets[:4]  # max 4 targets per source

        for i, tgt_id in enumerate(targets):
            tgt_flow = hour_preds[tgt_id][2]

            # probability: higher when source is more overloaded
            src_ci = hour_preds[src_id][6]
            tgt_ci = hour_preds[tgt_id][6]
            ci_diff = max(0, src_ci - tgt_ci)

            prob = round(min(0.35, 0.04 + ci_diff * 0.04 + (4 - i) * 0.03), 4)
            coupon = round(min(75.0, 5.0 + ci_diff * 8.0 + (4 - i) * 3.0), 2)
            expected = int(round(src_flow * prob * (0.5 + 0.5 * (i == 0))))

            if expected < 10:
                continue

            predict_hour = f"{base_date} {h:02d}:00:00"
            transitions.append((src_id, tgt_id, predict_hour, prob, expected, coupon))

# ═══════════════════════════════════════════════════════════
# Write SQL files
# ═══════════════════════════════════════════════════════════

def write_predictions_sql(filepath):
    lines = []
    lines.append("-- Seed data: locations_predictions (21 locations x 12 hours = 252 rows)")
    lines.append("-- Generated to match predictor.py data characteristics")
    lines.append("-- Base date: 2026-05-13 (Wednesday, no holiday)")
    lines.append("")
    lines.append("START TRANSACTION;")
    lines.append("")
    lines.append("INSERT INTO `locations_predictions`")
    lines.append("  (`location_id`, `predict_time`, `visitor_count`, `congestion_level`,")
    lines.append("   `trend`, `create_time`, `crowd_density`, `traffic_level`)")
    lines.append("VALUES")

    for i, p in enumerate(predictions):
        loc_id, predict_time, visitor_count, congestion_level, trend, create_time, crowd_density, traffic_level = p
        comma = "," if i < len(predictions) - 1 else ";"
        lines.append(
            f"  ({loc_id}, '{predict_time}', {visitor_count}, {congestion_level}, "
            f"'{trend}', '{create_time}', {crowd_density}, {traffic_level}){comma}"
        )

    lines.append("")
    lines.append("COMMIT;")
    lines.append("")

    with open(filepath, "w", encoding="utf-8") as f:
        f.write("\n".join(lines))
    print(f"Wrote {len(predictions)} rows to {filepath}")

def write_transitions_sql(filepath):
    lines = []
    lines.append("-- Seed data: location_transitions (diversion pairs)")
    lines.append("-- Generated to match run_diversion_strategy() output")
    lines.append("-- Only overloaded-source -> slack-target pairs with realistic probabilities")
    lines.append("")
    lines.append("START TRANSACTION;")
    lines.append("")
    lines.append("INSERT INTO `location_transitions`")
    lines.append("  (`source_location_id`, `target_location_id`, `predict_hour`,")
    lines.append("   `transition_probability`, `expected_users`, `coupon_amount`)")
    lines.append("VALUES")

    for i, t in enumerate(transitions):
        src_id, tgt_id, predict_hour, prob, expected, coupon = t
        comma = "," if i < len(transitions) - 1 else ";"
        lines.append(
            f"  ({src_id}, {tgt_id}, '{predict_hour}', {prob}, {expected}, {coupon}){comma}"
        )

    lines.append("")
    lines.append("COMMIT;")
    lines.append("")

    with open(filepath, "w", encoding="utf-8") as f:
        f.write("\n".join(lines))
    print(f"Wrote {len(transitions)} rows to {filepath}")

if __name__ == "__main__":
    import os
    out_dir = os.path.dirname(os.path.abspath(__file__))
    write_predictions_sql(os.path.join(out_dir, "seed_locations_predictions.sql"))
    write_transitions_sql(os.path.join(out_dir, "seed_location_transitions.sql"))
    print("Done.")
