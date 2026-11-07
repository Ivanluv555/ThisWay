#!/bin/bash

# API 健康检查脚本
# 使用方法: bash devs/script/test_api_health.sh <server-url>
# 示例: bash devs/script/test_api_health.sh http://localhost:8080

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# 服务器地址
SERVER_URL="${1:-http://localhost:8080}"

# 统计
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[✓]${NC} $1"
    ((PASSED_TESTS++))
    ((TOTAL_TESTS++))
}

log_error() {
    echo -e "${RED}[✗]${NC} $1"
    ((FAILED_TESTS++))
    ((TOTAL_TESTS++))
}

log_section() {
    echo ""
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}========================================${NC}"
}

# 测试 API 接口
test_api() {
    local method=$1
    local endpoint=$2
    local expected_status=$3
    local description=$4

    log_info "测试: $description"

    response=$(curl -s -w "\n%{http_code}" -X "$method" "$SERVER_URL$endpoint" 2>/dev/null || echo "000")
    status_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | head -n-1)

    if [ "$status_code" = "$expected_status" ]; then
        log_success "$description - 状态码: $status_code"
    else
        log_error "$description - 期望: $expected_status, 实际: $status_code"
        echo "响应: $body" | head -c 200
    fi
}

# 测试带 JSON 数据的 API
test_api_with_json() {
    local method=$1
    local endpoint=$2
    local json_data=$3
    local expected_status=$4
    local description=$5

    log_info "测试: $description"

    response=$(curl -s -w "\n%{http_code}" -X "$method" \
        -H "Content-Type: application/json" \
        -d "$json_data" \
        "$SERVER_URL$endpoint" 2>/dev/null || echo "000")

    status_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | head -n-1)

    if [ "$status_code" = "$expected_status" ]; then
        log_success "$description - 状态码: $status_code"
    else
        log_error "$description - 期望: $expected_status, 实际: $status_code"
        echo "响应: $body" | head -c 200
    fi
}

main() {
    echo ""
    echo "======================================"
    echo "  API 健康检查"
    echo "  服务器: $SERVER_URL"
    echo "======================================"
    echo ""

    # 1. 基础连通性测试
    log_section "1. 基础连通性测试"

    log_info "检查服务器连通性..."
    if curl -s --connect-timeout 5 "$SERVER_URL" > /dev/null 2>&1; then
        log_success "服务器可访问"
        ((PASSED_TESTS++))
        ((TOTAL_TESTS++))
    else
        log_error "服务器不可访问"
        ((FAILED_TESTS++))
        ((TOTAL_TESTS++))
        echo ""
        echo "请检查:"
        echo "  1. 服务器是否启动"
        echo "  2. 端口是否正确"
        echo "  3. 防火墙是否开放"
        exit 1
    fi

    # 2. 核心接口测试
    log_section "2. 核心接口测试"

    test_api "GET" "/api/posts" "200" "获取动态列表"
    test_api "GET" "/api/locations" "200" "获取景区列表"
    test_api "GET" "/api/location-images" "200" "获取景区图片"

    # 3. API 文档测试
    log_section "3. API 文档测试"

    test_api "GET" "/doc.html" "200" "Swagger 文档访问"
    test_api "GET" "/v3/api-docs" "200" "OpenAPI 文档访问"

    # 4. 健康检查接口
    log_section "4. 健康检查接口"

    test_api "GET" "/actuator/health" "404" "健康检查接口 (未启用则 404)"

    # 5. 错误处理测试
    log_section "5. 错误处理测试"

    test_api "GET" "/api/nonexistent" "404" "不存在的接口返回 404"
    test_api "GET" "/api/posts/999999" "404" "不存在的资源返回 404"

    # 6. 性能测试
    log_section "6. 性能测试"

    log_info "测试接口响应时间..."
    start_time=$(date +%s%N)
    curl -s "$SERVER_URL/api/locations" > /dev/null
    end_time=$(date +%s%N)
    response_time=$(( (end_time - start_time) / 1000000 ))

    if [ "$response_time" -lt 2000 ]; then
        log_success "接口响应时间: ${response_time}ms (< 2s)"
        ((PASSED_TESTS++))
    else
        log_error "接口响应时间: ${response_time}ms (> 2s)"
        ((FAILED_TESTS++))
    fi
    ((TOTAL_TESTS++))

    # 输出统计结果
    log_section "测试结果统计"
    echo ""
    echo "  总测试数: $TOTAL_TESTS"
    echo -e "  ${GREEN}通过: $PASSED_TESTS${NC}"
    echo -e "  ${RED}失败: $FAILED_TESTS${NC}"
    echo ""

    if [ "$FAILED_TESTS" -eq 0 ]; then
        echo -e "${GREEN}✓ 所有测试通过！${NC}"
        exit 0
    else
        echo -e "${RED}✗ 有 $FAILED_TESTS 个测试失败${NC}"
        exit 1
    fi
}

main
