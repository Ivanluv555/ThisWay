#!/bin/bash
# ============================================================================
# 生产启动脚本 - 使用本地数据库 (localhost)
# ============================================================================

set -e

echo "=== CIDI Server 生产部署 ==="

# ========== 必填配置检查 ==========
if [ -z "$SDP" ]; then
    echo "❌ 错误：数据库密码未设置"
    echo "请设置：export SDP='your_password'"
    exit 1
fi

if [ -z "$WX_APPID" ]; then
    echo "❌ 错误：微信 AppID 未设置"
    echo "请设置：export WX_APPID='wx1234567890abcdef'"
    exit 1
fi

if [ -z "$WX_SECRET" ]; then
    echo "❌ 错误：微信 Secret 未设置"
    echo "请设置：export WX_SECRET='your_wechat_secret'"
    exit 1
fi

if [ -z "$JWT_SECRET" ]; then
    echo "⚠️  警告：JWT 密钥未设置，自动生成..."
    export JWT_SECRET="$(openssl rand -base64 32)"
    echo "生成的 JWT 密钥: ${JWT_SECRET:0:20}..."
fi

# ========== 配置信息 ==========
echo ""
echo "配置信息："
echo "  数据库地址: localhost:3306/cidi-db"
echo "  微信 AppID: ${WX_APPID:0:10}..."
echo "  JWT 密钥: ${JWT_SECRET:0:20}..."
echo ""

# ========== 打包 ==========
echo "[1/3] 打包项目..."
./mvnw clean package -DskipTests

# ========== 检查 JAR 文件 ==========
JAR_FILE="target/cidi-0.0.1-SNAPSHOT.jar"
if [ ! -f "$JAR_FILE" ]; then
    echo "❌ 错误：JAR 文件不存在: $JAR_FILE"
    exit 1
fi

echo "[2/3] 打包完成: $JAR_FILE"

# ========== 启动服务 ==========
echo "[3/3] 启动服务..."
echo ""
java -jar "$JAR_FILE"
