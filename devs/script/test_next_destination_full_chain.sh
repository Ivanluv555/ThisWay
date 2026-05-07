#!/bin/bash

# 下一目的地预测数据链路完整测试脚本
# 用途: 验证从数据生成到前端展示的完整链路

set -e

echo "=========================================="
echo "下一目的地预测数据链路完整测试"
echo "=========================================="
echo ""

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 配置
SPRING_BOOT_URL="http://localhost:8080"
FASTAPI_URL="http://localhost:8001"
TEST_LOCATION_ID=1

# 测试计数
PASSED=0
FAILED=0

# 测试函数
test_api() {
    local name=$1
    local url=$2
    local expected_status=${3:-200}

    echo -n "测试: $name ... "

    response=$(curl -s -w "\n%{http_code}" "$url" 2>/dev/null)
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')

    if [ "$http_code" -eq "$expected_status" ]; then
        echo -e "${GREEN}✓ 通过${NC} (HTTP $http_code)"
        PASSED=$((PASSED + 1))
        return 0
    else
        echo -e "${RED}✗ 失败${NC} (HTTP $http_code, 期望 $expected_status)"
        FAILED=$((FAILED + 1))
        return 1
    fi
}

test_api_with_data() {
    local name=$1
    local url=$2
    local min_count=${3:-1}

    echo -n "测试: $name ... "

    response=$(curl -s "$url" 2>/dev/null)

    if [ -z "$response" ]; then
        echo -e "${RED}✗ 失败${NC} (无响应)"
        FAILED=$((FAILED + 1))
        return 1
    fi

    # 检查是否是数组且有数据
    count=$(echo "$response" | jq 'length' 2>/dev/null || echo "0")

    if [ "$count" -ge "$min_count" ]; then
        echo -e "${GREEN}✓ 通过${NC} (返回 $count 条数据)"
        PASSED=$((PASSED + 1))
        return 0
    else
        echo -e "${YELLOW}⚠ 警告${NC} (返回 $count 条数据，期望至少 $min_count 条)"
        FAILED=$((FAILED + 1))
        return 1
    fi
}

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "第 1 步: 检查服务状态"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

test_api "Spring Boot 健康检查" "$SPRING_BOOT_URL/api/locations" 200
test_api "FastAPI 健康检查" "$FASTAPI_URL/health" 200

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "第 2 步: 检查基础数据"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

test_api_with_data "景点列表" "$SPRING_BOOT_URL/api/locations" 1

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "第 3 步: 生成历史数据（如果没有）"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# 检查是否有历史数据
echo -n "检查历史人流数据 ... "
history_count=$(curl -s "$SPRING_BOOT_URL/api/flow/history?hours=24" | jq 'length' 2>/dev/null || echo "0")

if [ "$history_count" -lt 10 ]; then
    echo -e "${YELLOW}数据不足 ($history_count 条)${NC}"
    echo "正在生成 24 小时历史数据 ..."

    response=$(curl -s -X POST "$FASTAPI_URL/flow/simulation/batch-insert?hours=24&lookback=true")

    if echo "$response" | jq -e '.status == "success"' > /dev/null 2>&1; then
        inserted=$(echo "$response" | jq -r '.inserted // 0')
        echo -e "${GREEN}✓ 成功生成 $inserted 条历史数据${NC}"
        PASSED=$((PASSED + 1))
    else
        echo -e "${RED}✗ 生成历史数据失败${NC}"
        echo "响应: $response"
        FAILED=$((FAILED + 1))
    fi
else
    echo -e "${GREEN}✓ 已有 $history_count 条数据${NC}"
    PASSED=$((PASSED + 1))
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "第 4 步: 触发 AI 预测"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

echo "正在调用 FastAPI /predict 接口 ..."
predict_response=$(curl -s -X POST "$FASTAPI_URL/predict?write_back=true")

if echo "$predict_response" | jq -e '.summary' > /dev/null 2>&1; then
    spot_count=$(echo "$predict_response" | jq -r '.summary.spotCount // 0')
    predict_time=$(echo "$predict_response" | jq -r '.summary.predictTime // "unknown"')
    echo -e "${GREEN}✓ 预测成功${NC}"
    echo "  - 景点数量: $spot_count"
    echo "  - 预测时间: $predict_time"
    PASSED=$((PASSED + 1))
else
    echo -e "${RED}✗ 预测失败${NC}"
    echo "响应: $predict_response"
    FAILED=$((FAILED + 1))
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "第 5 步: 验证预测数据写入"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

test_api_with_data "人流预测数据" "$SPRING_BOOT_URL/api/spot-traffic-predictions/latest" 1
test_api_with_data "转移预测数据" "$SPRING_BOOT_URL/api/location-transitions" 1

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "第 6 步: 测试前端 API"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

echo -n "测试: 下一目的地预测 API ... "
next_dest_response=$(curl -s "$SPRING_BOOT_URL/api/location-transitions/next-destinations/$TEST_LOCATION_ID?hours=6")

if echo "$next_dest_response" | jq -e 'type == "array"' > /dev/null 2>&1; then
    count=$(echo "$next_dest_response" | jq 'length')

    if [ "$count" -gt 0 ]; then
        echo -e "${GREEN}✓ 通过${NC} (返回 $count 个时间点的预测)"

        # 显示第一个预测详情
        first_hour=$(echo "$next_dest_response" | jq -r '.[0].hour // "unknown"')
        first_transitions=$(echo "$next_dest_response" | jq -r '.[0].transitions | length')
        echo "  - 第一个时间点: $first_hour"
        echo "  - 推荐目的地数: $first_transitions"

        # 显示前3个推荐
        echo "  - 推荐详情:"
        echo "$next_dest_response" | jq -r '.[0].transitions[0:3][] | "    → \(.locationName): \(.ratioText) (\(.users)人)"' 2>/dev/null || true

        PASSED=$((PASSED + 1))
    else
        echo -e "${YELLOW}⚠ 警告${NC} (返回空数组)"
        echo "  可能原因: 该景点没有转移预测数据"
        FAILED=$((FAILED + 1))
    fi
else
    echo -e "${RED}✗ 失败${NC}"
    echo "响应: $next_dest_response"
    FAILED=$((FAILED + 1))
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "第 7 步: 数据库验证"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

if command -v mysql &> /dev/null; then
    echo -n "检查 location_transitions 表数据量 ... "

    # 尝试连接数据库（需要配置密码）
    db_count=$(mysql -u root -p'Lby20050125.' -e "USE cidi_db; SELECT COUNT(*) as count FROM location_transitions;" -s -N 2>/dev/null || echo "0")

    if [ "$db_count" -gt 0 ]; then
        echo -e "${GREEN}✓ 表中有 $db_count 条记录${NC}"
        PASSED=$((PASSED + 1))
    else
        echo -e "${YELLOW}⚠ 表中没有数据${NC}"
        FAILED=$((FAILED + 1))
    fi
else
    echo -e "${YELLOW}⚠ 跳过${NC} (mysql 命令不可用)"
fi

echo ""
echo "=========================================="
echo "测试总结"
echo "=========================================="
echo ""
echo -e "通过: ${GREEN}$PASSED${NC}"
echo -e "失败: ${RED}$FAILED${NC}"
echo ""

if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}✓ 所有测试通过！数据链路正常工作。${NC}"
    exit 0
else
    echo -e "${RED}✗ 有 $FAILED 个测试失败，请检查上述错误信息。${NC}"
    exit 1
fi
