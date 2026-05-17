#!/bin/bash

# 一键部署脚本
# 使用方法: bash devs/script/deploy.sh <environment>
# 示例: bash devs/script/deploy.sh prod

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# 环境参数
ENVIRONMENT="${1:-dev}"
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[✓]${NC} $1"
}

log_error() {
    echo -e "${RED}[✗]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[⚠]${NC} $1"
}

log_section() {
    echo ""
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}========================================${NC}"
}

# 检查环境变量
check_env_vars() {
    log_section "检查环境变量"

    local required_vars=(
        "SPRING_DATASOURCE_URL"
        "SPRING_DATASOURCE_USERNAME"
        "SPRING_DATASOURCE_PASSWORD"
    )

    if [ "$ENVIRONMENT" = "prod" ]; then
        required_vars+=(
            "WX_APPID_PROD"
            "WX_SECRET_PROD"
            "JWT_SECRET_PROD"
        )
    fi

    local missing_vars=()
    for var in "${required_vars[@]}"; do
        if [ -z "${!var}" ]; then
            missing_vars+=("$var")
        else
            log_success "$var 已配置"
        fi
    done

    if [ ${#missing_vars[@]} -gt 0 ]; then
        log_error "缺少以下环境变量:"
        for var in "${missing_vars[@]}"; do
            echo "  - $var"
        done
        exit 1
    fi
}

# 运行部署前检查
run_pre_deploy_check() {
    log_section "运行部署前检查"

    if [ -f "$PROJECT_ROOT/devs/script/pre_deploy_check.sh" ]; then
        bash "$PROJECT_ROOT/devs/script/pre_deploy_check.sh"
        if [ $? -ne 0 ]; then
            log_error "部署前检查失败，请修复问题后重试"
            exit 1
        fi
    else
        log_warning "未找到部署前检查脚本，跳过"
    fi
}

# 备份数据库
backup_database() {
    log_section "备份数据库"

    if [ "$ENVIRONMENT" = "prod" ]; then
        log_info "生产环境，执行数据库备份..."

        BACKUP_DIR="$PROJECT_ROOT/backups"
        mkdir -p "$BACKUP_DIR"

        BACKUP_FILE="$BACKUP_DIR/cidi_db_$(date +%Y%m%d_%H%M%S).sql"

        # 从环境变量解析数据库信息
        DB_HOST=$(echo "$SPRING_DATASOURCE_URL" | sed -n 's/.*:\/\/\([^:]*\):.*/\1/p')
        DB_PORT=$(echo "$SPRING_DATASOURCE_URL" | sed -n 's/.*:\([0-9]*\)\/.*/\1/p')
        DB_NAME=$(echo "$SPRING_DATASOURCE_URL" | sed -n 's/.*\/\([^?]*\).*/\1/p')

        log_info "备份数据库: $DB_NAME@$DB_HOST:$DB_PORT"

        mysqldump -h "$DB_HOST" -P "$DB_PORT" \
            -u "$SPRING_DATASOURCE_USERNAME" \
            -p"$SPRING_DATASOURCE_PASSWORD" \
            "$DB_NAME" > "$BACKUP_FILE" 2>/dev/null

        if [ $? -eq 0 ]; then
            log_success "数据库备份成功: $BACKUP_FILE"
        else
            log_error "数据库备份失败"
            exit 1
        fi
    else
        log_info "非生产环境，跳过数据库备份"
    fi
}

# 构建 Java 后端
build_java_backend() {
    log_section "构建 Java 后端"

    cd "$PROJECT_ROOT/devs/backend/cidi-server"

    log_info "清理旧构建..."
    ./mvnw clean

    log_info "开始构建..."
    ./mvnw package -DskipTests

    if [ $? -eq 0 ]; then
        JAR_FILE=$(ls target/*.jar | head -1)
        JAR_SIZE=$(du -h "$JAR_FILE" | cut -f1)
        log_success "构建成功: $JAR_FILE ($JAR_SIZE)"
    else
        log_error "构建失败"
        exit 1
    fi

    cd "$PROJECT_ROOT"
}

# 停止旧服务
stop_old_service() {
    log_section "停止旧服务"

    log_info "查找运行中的 Java 进程..."
    OLD_PID=$(ps aux | grep "cidi.*\.jar" | grep -v grep | awk '{print $2}')

    if [ -n "$OLD_PID" ]; then
        log_info "发现运行中的服务 (PID: $OLD_PID)，正在停止..."
        kill "$OLD_PID"
        sleep 3

        # 检查是否还在运行
        if ps -p "$OLD_PID" > /dev/null 2>&1; then
            log_warning "服务未停止，强制终止..."
            kill -9 "$OLD_PID"
        fi

        log_success "旧服务已停止"
    else
        log_info "未发现运行中的服务"
    fi
}

# 启动新服务
start_new_service() {
    log_section "启动新服务"

    cd "$PROJECT_ROOT/devs/backend/cidi-server"

    JAR_FILE=$(ls target/*.jar | head -1)
    LOG_DIR="$PROJECT_ROOT/logs"
    mkdir -p "$LOG_DIR"

    log_info "启动服务: $JAR_FILE"
    log_info "环境: $ENVIRONMENT"
    log_info "日志目录: $LOG_DIR"

    export SPRING_PROFILES_ACTIVE="$ENVIRONMENT"

    nohup java -jar "$JAR_FILE" \
        > "$LOG_DIR/app.log" 2>&1 &

    NEW_PID=$!
    echo "$NEW_PID" > "$LOG_DIR/app.pid"

    log_success "服务已启动 (PID: $NEW_PID)"

    cd "$PROJECT_ROOT"
}

# 健康检查
health_check() {
    log_section "健康检查"

    log_info "等待服务启动..."
    sleep 10

    MAX_RETRIES=12
    RETRY_COUNT=0

    while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
        log_info "尝试连接服务 ($((RETRY_COUNT + 1))/$MAX_RETRIES)..."

        if curl -s http://localhost:8080/api/posts > /dev/null 2>&1; then
            log_success "服务启动成功！"

            # 运行 API 健康检查
            if [ -f "$PROJECT_ROOT/devs/script/test_api_health.sh" ]; then
                bash "$PROJECT_ROOT/devs/script/test_api_health.sh" http://localhost:8080
            fi

            return 0
        fi

        sleep 5
        ((RETRY_COUNT++))
    done

    log_error "服务启动失败或健康检查超时"
    log_info "查看日志: tail -f $PROJECT_ROOT/logs/app.log"
    exit 1
}

# 部署总结
deployment_summary() {
    log_section "部署总结"

    echo ""
    echo "  环境: $ENVIRONMENT"
    echo "  时间: $(date '+%Y-%m-%d %H:%M:%S')"
    echo "  服务地址: http://localhost:8080"
    echo "  API 文档: http://localhost:8080/doc.html"
    echo "  日志文件: $PROJECT_ROOT/logs/app.log"
    echo "  PID 文件: $PROJECT_ROOT/logs/app.pid"
    echo ""
    echo "常用命令:"
    echo "  查看日志: tail -f $PROJECT_ROOT/logs/app.log"
    echo "  停止服务: kill \$(cat $PROJECT_ROOT/logs/app.pid)"
    echo "  重启服务: bash $PROJECT_ROOT/devs/script/deploy.sh $ENVIRONMENT"
    echo ""
}

# 主函数
main() {
    echo ""
    echo "======================================"
    echo "  一键部署脚本"
    echo "  环境: $ENVIRONMENT"
    echo "======================================"
    echo ""

    if [ "$ENVIRONMENT" = "prod" ]; then
        log_warning "即将部署到生产环境，请确认："
        echo "  1. 已完成所有测试"
        echo "  2. 已配置所有环境变量"
        echo "  3. 已通知相关人员"
        echo ""
        read -p "确认继续部署？(yes/no): " confirm
        if [ "$confirm" != "yes" ]; then
            log_info "部署已取消"
            exit 0
        fi
    fi

    check_env_vars
    run_pre_deploy_check
    backup_database
    build_java_backend
    stop_old_service
    start_new_service
    health_check
    deployment_summary

    log_success "部署完成！"
}

main
