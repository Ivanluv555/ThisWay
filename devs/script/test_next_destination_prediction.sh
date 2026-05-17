#!/bin/bash
# 测试下一目的地预测功能的完整数据流

echo "=========================================="
echo "测试：下一目的地预测功能"
echo "=========================================="
echo ""

# 配置
SPRING_BASE_URL="http://localhost:8080"
FASTAPI_BASE_URL="http://localhost:8001"

echo "1. 检查数据库表是否存在..."
mysql -h 47.111.180.134 -u root -p${SDP} -D cidi-db -e "DESC location_transitions;" 2>&1 | grep -v Warning
if [ $? -eq 0 ]; then
    echo "✓ 数据库表 location_transitions 已创建"
else
    echo "✗ 数据库表不存在"
    exit 1
fi
echo ""

echo "2. 检查 Spring Boot 服务..."
curl -s ${SPRING_BASE_URL}/health > /dev/null 2>&1
if [ $? -eq 0 ]; then
    echo "✓ Spring Boot 服务运行中"
else
    echo "✗ Spring Boot 服务未启动，请先启动: cd backend/cidi-server && ./mvnw spring-boot:run"
    exit 1
fi
echo ""

echo "3. 检查 FastAPI 服务..."
curl -s ${FASTAPI_BASE_URL}/health > /dev/null 2>&1
if [ $? -eq 0 ]; then
    echo "✓ FastAPI 服务运行中"
else
    echo "✗ FastAPI 服务未启动，请先启动: cd backend/cidi-model && uvicorn main:app --host 0.0.0.0 --port 8001"
    exit 1
fi
echo ""

echo "4. 触发 AI 模型预测（会自动写入转移数据）..."
curl -s -X POST "${FASTAPI_BASE_URL}/predict?write_back=true" \
    -H "Content-Type: application/json" | jq -r '.summary'
if [ $? -eq 0 ]; then
    echo "✓ 预测任务执行成功"
else
    echo "✗ 预测任务失败"
    exit 1
fi
echo ""

echo "5. 检查转移数据是否写入数据库..."
COUNT=$(mysql -h 47.111.180.134 -u root -p${SDP} -D cidi-db -se "SELECT COUNT(*) FROM location_transitions;" 2>&1 | grep -v Warning)
echo "   数据库中共有 ${COUNT} 条转移预测记录"
if [ "$COUNT" -gt 0 ]; then
    echo "✓ 转移数据已写入"
else
    echo "✗ 转移数据未写入"
    exit 1
fi
echo ""

echo "6. 测试前端 API 接口（获取景点1的下一目的地预测）..."
RESPONSE=$(curl -s "${SPRING_BASE_URL}/api/location-transitions/next-destinations/1?hours=6")
echo "$RESPONSE" | jq '.[0]' 2>/dev/null
if [ $? -eq 0 ]; then
    echo "✓ API 接口返回正常"
else
    echo "✗ API 接口返回异常"
    echo "$RESPONSE"
    exit 1
fi
echo ""

echo "=========================================="
echo "✓ 所有测试通过！"
echo "=========================================="
echo ""
echo "数据流验证："
echo "  AI模型 → Spring Boot → 数据库 → 前端API"
echo ""
echo "下一步："
echo "  1. 前端调用 /api/location-transitions/next-destinations/{spotId}"
echo "  2. 查看实时预测数据"
