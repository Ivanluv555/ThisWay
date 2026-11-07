#!/bin/bash

# 项目上线前自动化检查脚本
# 使用方法: bash devs/script/pre_deploy_check.sh

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 项目根目录
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$PROJECT_ROOT"

# 检查结果统计
TOTAL_CHECKS=0
PASSED_CHECKS=0
FAILED_CHECKS=0
WARNING_CHECKS=0

# 日志函数
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[✓]${NC} $1"
    ((PASSED_CHECKS++))
    ((TOTAL_CHECKS++))
}

log_warning() {
    echo -e "${YELLOW}[⚠]${NC} $1"
    ((WARNING_CHECKS++))
    ((TOTAL_CHECKS++))
}

log_error() {
    echo -e "${RED}[✗]${NC} $1"
    ((FAILED_CHECKS++))
    ((TOTAL_CHECKS++))
}

log_section() {
    echo ""
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}========================================${NC}"
}

# 1. 代码质量检查
check_code_quality() {
    log_section "1. 代码质量检查"

    # 检查 TODO/FIXME
    log_info "检查 TODO/FIXME 注释..."
    TODO_COUNT=$(grep -r "TODO\|FIXME\|XXX\|HACK" devs/backend/cidi-server/src/main/java --include="*.java" 2>/dev/null | wc -l || echo 0)
    if [ "$TODO_COUNT" -eq 0 ]; then
        log_success "Java 代码无 TODO/FIXME 注释"
    else
        log_warning "发现 $TODO_COUNT 个 TODO/FIXME 注释，建议处理"
        grep -r "TODO\|FIXME\|XXX\|HACK" devs/backend/cidi-server/src/main/java --include="*.java" 2>/dev/null | head -5
    fi

    # 检查 Python TODO
    if [ -d "devs/backend/cidi-model" ]; then
        TODO_PY_COUNT=$(grep -r "TODO\|FIXME\|XXX\|HACK" devs/backend/cidi-model --include="*.py" 2>/dev/null | wc -l || echo 0)
        if [ "$TODO_PY_COUNT" -eq 0 ]; then
            log_success "Python 代码无 TODO/FIXME 注释"
        else
            log_warning "发现 $TODO_PY_COUNT 个 Python TODO/FIXME 注释"
        fi
    fi

    # 检查调试输出
    log_info "检查调试输出..."
    DEBUG_JAVA=$(grep -r "System.out.println\|printStackTrace" devs/backend/cidi-server/src/main/java --include="*.java" 2>/dev/null | wc -l || echo 0)
    if [ "$DEBUG_JAVA" -eq 0 ]; then
        log_success "Java 代码无调试输出"
    else
        log_error "发现 $DEBUG_JAVA 处 System.out.println 或 printStackTrace"
        grep -r "System.out.println\|printStackTrace" devs/backend/cidi-server/src/main/java --include="*.java" 2>/dev/null | head -3
    fi

    DEBUG_JS=$(grep -r "console.log\|console.error" devs/frontend/cidi-wx --include="*.js" 2>/dev/null | grep -v "// console" | wc -l || echo 0)
    if [ "$DEBUG_JS" -eq 0 ]; then
        log_success "前端代码无 console.log"
    else
        log_warning "发现 $DEBUG_JS 处 console.log，建议移除"
    fi
}

# 2. 安全检查
check_security() {
    log_section "2. 安全检查"

    # 检查硬编码密码
    log_info "检查硬编码密码..."
    HARDCODED_PWD=$(grep -r "password\s*=\s*['\"]" devs/backend/cidi-server/src/main/resources --include="*.properties" --include="*.yml" 2>/dev/null | grep -v "^\s*#" | grep -v "\${" | wc -l || echo 0)
    if [ "$HARDCODED_PWD" -eq 0 ]; then
        log_success "配置文件无硬编码密码"
    else
        log_error "发现硬编码密码，必须使用环境变量"
        grep -r "password\s*=\s*['\"]" devs/backend/cidi-server/src/main/resources --include="*.properties" 2>/dev/null | grep -v "^\s*#" | grep -v "\${"
    fi

    # 检查硬编码 Secret
    log_info "检查硬编码 Secret..."
    HARDCODED_SECRET=$(grep -r "secret\s*=\s*['\"]" devs/backend/cidi-server/src/main/resources --include="*.properties" 2>/dev/null | grep -v "^\s*#" | grep -v "\${" | wc -l || echo 0)
    if [ "$HARDCODED_SECRET" -eq 0 ]; then
        log_success "配置文件无硬编码 Secret"
    else
        log_error "发现硬编码 Secret，必须使用环境变量"
    fi

    # 检查是否有敏感文件被提交
    log_info "检查敏感文件..."
    SENSITIVE_FILES=$(git ls-files | grep -E "\.env$|\.pem$|\.key$|secret|credential" | wc -l || echo 0)
    if [ "$SENSITIVE_FILES" -eq 0 ]; then
        log_success "无敏感文件被提交"
    else
        log_error "发现敏感文件被提交到 Git"
        git ls-files | grep -E "\.env$|\.pem$|\.key$|secret|credential"
    fi

    # 检查 SQL 注入风险
    log_info "检查 SQL 拼接..."
    SQL_CONCAT=$(grep -r "\"SELECT.*+\|\"INSERT.*+\|\"UPDATE.*+\|\"DELETE.*+" devs/backend/cidi-server/src/main/java --include="*.java" 2>/dev/null | wc -l || echo 0)
    if [ "$SQL_CONCAT" -eq 0 ]; then
        log_success "无 SQL 字符串拼接"
    else
        log_error "发现 $SQL_CONCAT 处 SQL 字符串拼接，存在注入风险"
        grep -r "\"SELECT.*+\|\"INSERT.*+\|\"UPDATE.*+\|\"DELETE.*+" devs/backend/cidi-server/src/main/java --include="*.java" 2>/dev/null | head -3
    fi
}

# 3. 配置检查
check_configuration() {
    log_section "3. 配置检查"

    # 检查生产配置文件
    log_info "检查生产配置文件..."
    if [ -f "devs/backend/cidi-server/src/main/resources/application-prod.properties" ]; then
        log_success "生产配置文件存在"

        # 检查是否使用环境变量
        ENV_VAR_COUNT=$(grep "\${" devs/backend/cidi-server/src/main/resources/application-prod.properties | wc -l)
        if [ "$ENV_VAR_COUNT" -gt 0 ]; then
            log_success "生产配置使用环境变量"
        else
            log_warning "生产配置未使用环境变量"
        fi
    else
        log_error "生产配置文件不存在"
    fi

    # 检查 JPA 配置
    log_info "检查 JPA 配置..."
    if grep -q "spring.jpa.show-sql=true" devs/backend/cidi-server/src/main/resources/application-prod.properties 2>/dev/null; then
        log_error "生产环境不应启用 show-sql"
    else
        log_success "JPA show-sql 配置正确"
    fi

    if grep -q "spring.jpa.hibernate.ddl-auto=update" devs/backend/cidi-server/src/main/resources/application-prod.properties 2>/dev/null; then
        log_warning "生产环境建议使用 ddl-auto=validate"
    else
        log_success "JPA ddl-auto 配置正确"
    fi

    # 检查前端 API 地址
    log_info "检查前端 API 地址..."
    if grep -q "localhost\|127.0.0.1" devs/frontend/cidi-wx/app.js 2>/dev/null; then
        log_warning "前端 API 地址仍为 localhost，上线前需修改"
        grep "apiBaseUrl" devs/frontend/cidi-wx/app.js
    else
        log_success "前端 API 地址配置正确"
    fi
}

# 4. 依赖检查
check_dependencies() {
    log_section "4. 依赖检查"

    # 检查 Java 依赖
    log_info "检查 Java 依赖..."
    if [ -f "devs/backend/cidi-server/pom.xml" ]; then
        log_success "pom.xml 存在"

        # 检查是否有 SNAPSHOT 依赖
        SNAPSHOT_COUNT=$(grep -c "SNAPSHOT" devs/backend/cidi-server/pom.xml || echo 0)
        if [ "$SNAPSHOT_COUNT" -eq 0 ]; then
            log_success "无 SNAPSHOT 依赖"
        else
            log_warning "发现 $SNAPSHOT_COUNT 个 SNAPSHOT 依赖，生产环境建议使用稳定版本"
        fi
    fi

    # 检查 Python 依赖
    log_info "检查 Python 依赖..."
    if [ -f "devs/backend/cidi-model/requirements.txt" ]; then
        log_success "requirements.txt 存在"

        # 检查是否有版本号
        NO_VERSION=$(grep -v "^#" devs/backend/cidi-model/requirements.txt | grep -v "^$" | grep -v ">=" | wc -l || echo 0)
        if [ "$NO_VERSION" -eq 0 ]; then
            log_success "所有 Python 依赖都有版本号"
        else
            log_warning "部分 Python 依赖未指定版本号"
        fi
    fi
}

# 5. 构建测试
check_build() {
    log_section "5. 构建测试"

    # Java 构建
    log_info "测试 Java 构建..."
    if [ -f "devs/backend/cidi-server/pom.xml" ]; then
        cd devs/backend/cidi-server
        if ./mvnw clean package -DskipTests > /tmp/maven_build.log 2>&1; then
            JAR_SIZE=$(du -h target/*.jar 2>/dev/null | cut -f1 || echo "unknown")
            log_success "Java 构建成功，JAR 大小: $JAR_SIZE"
        else
            log_error "Java 构建失败，查看日志: /tmp/maven_build.log"
            tail -20 /tmp/maven_build.log
        fi
        cd "$PROJECT_ROOT"
    fi
}

# 6. Git 状态检查
check_git_status() {
    log_section "6. Git 状态检查"

    # 检查未提交的更改
    log_info "检查 Git 状态..."
    UNCOMMITTED=$(git status --porcelain | wc -l)
    if [ "$UNCOMMITTED" -eq 0 ]; then
        log_success "无未提交的更改"
    else
        log_warning "有 $UNCOMMITTED 个未提交的更改"
        git status --short | head -10
    fi

    # 检查当前分支
    CURRENT_BRANCH=$(git branch --show-current)
    log_info "当前分支: $CURRENT_BRANCH"
    if [ "$CURRENT_BRANCH" = "master" ] || [ "$CURRENT_BRANCH" = "main" ]; then
        log_success "在主分支上"
    else
        log_warning "不在主分支上，当前分支: $CURRENT_BRANCH"
    fi

    # 检查是否有未推送的提交
    UNPUSHED=$(git log @{u}.. --oneline 2>/dev/null | wc -l || echo 0)
    if [ "$UNPUSHED" -eq 0 ]; then
        log_success "无未推送的提交"
    else
        log_warning "有 $UNPUSHED 个未推送的提交"
    fi
}

# 7. 文档检查
check_documentation() {
    log_section "7. 文档检查"

    # 检查必要文档
    REQUIRED_DOCS=("docs/README.md" "docs/02-开发与运行.md" "docs/06-部署与验证.md")

    for doc in "${REQUIRED_DOCS[@]}"; do
        if [ -f "$doc" ]; then
            log_success "文档存在: $doc"
        else
            log_warning "文档缺失: $doc"
        fi
    done
}

# 8. 数据库检查
check_database() {
    log_section "8. 数据库检查"

    # 检查数据库脚本
    log_info "检查数据库脚本..."
    if [ -f "devs/script/sql/cidi_db.sql" ]; then
        log_success "数据库初始化脚本存在"

        # 检查字符集
        if grep -q "utf8mb4" devs/script/sql/cidi_db.sql; then
            log_success "数据库使用 utf8mb4 字符集"
        else
            log_warning "数据库脚本未指定 utf8mb4 字符集"
        fi
    else
        log_warning "数据库初始化脚本不存在"
    fi
}

# 9. 性能检查
check_performance() {
    log_section "9. 性能检查"

    # 检查分页
    log_info "检查分页实现..."
    PAGEABLE_COUNT=$(grep -r "Pageable\|PageRequest" devs/backend/cidi-server/src/main/java --include="*.java" 2>/dev/null | wc -l || echo 0)
    if [ "$PAGEABLE_COUNT" -gt 0 ]; then
        log_success "发现 $PAGEABLE_COUNT 处分页实现"
    else
        log_warning "未发现分页实现，大数据量接口可能有性能问题"
    fi

    # 检查缓存
    log_info "检查缓存实现..."
    CACHE_COUNT=$(grep -r "@Cacheable\|@CacheEvict\|RedisTemplate" devs/backend/cidi-server/src/main/java --include="*.java" 2>/dev/null | wc -l || echo 0)
    if [ "$CACHE_COUNT" -gt 0 ]; then
        log_success "发现 $CACHE_COUNT 处缓存实现"
    else
        log_warning "未发现缓存实现，建议对热点数据进行缓存"
    fi

    # 检查前端图片大小
    log_info "检查前端图片大小..."
    LARGE_IMAGES=$(find devs/frontend/cidi-wx/images -type f -size +500k 2>/dev/null | wc -l || echo 0)
    if [ "$LARGE_IMAGES" -eq 0 ]; then
        log_success "无超大图片"
    else
        log_warning "发现 $LARGE_IMAGES 个超过 500KB 的图片，建议压缩"
        find devs/frontend/cidi-wx/images -type f -size +500k 2>/dev/null | head -5
    fi
}

# 10. 环境变量检查
check_environment_variables() {
    log_section "10. 环境变量检查清单"

    log_info "生产环境需要配置以下环境变量："
    echo ""
    echo "  必需变量:"
    echo "    - SPRING_PROFILES_ACTIVE=prod"
    echo "    - SPRING_DATASOURCE_URL=jdbc:mysql://<host>:3306/cidi-db?useSSL=false&serverTimezone=UTC"
    echo "    - SPRING_DATASOURCE_USERNAME=<db-user>"
    echo "    - SPRING_DATASOURCE_PASSWORD=<db-password>"
    echo "    - WX_APPID_PROD=<wechat-appid>"
    echo "    - WX_SECRET_PROD=<wechat-secret>"
    echo "    - JWT_SECRET_PROD=<strong-jwt-secret>"
    echo ""
    echo "  可选变量:"
    echo "    - JWT_EXPIRATION=604800000"
    echo ""
    log_warning "请确保在服务器上配置了所有必需的环境变量"
}

# 主函数
main() {
    echo ""
    echo "======================================"
    echo "  项目上线前自动化检查"
    echo "======================================"
    echo ""

    check_code_quality
    check_security
    check_configuration
    check_dependencies
    check_build
    check_git_status
    check_documentation
    check_database
    check_performance
    check_environment_variables

    # 输出统计结果
    log_section "检查结果统计"
    echo ""
    echo "  总检查项: $TOTAL_CHECKS"
    echo -e "  ${GREEN}通过: $PASSED_CHECKS${NC}"
    echo -e "  ${YELLOW}警告: $WARNING_CHECKS${NC}"
    echo -e "  ${RED}失败: $FAILED_CHECKS${NC}"
    echo ""

    if [ "$FAILED_CHECKS" -eq 0 ]; then
        if [ "$WARNING_CHECKS" -eq 0 ]; then
            echo -e "${GREEN}✓ 所有检查通过，可以上线！${NC}"
            exit 0
        else
            echo -e "${YELLOW}⚠ 有 $WARNING_CHECKS 个警告项，建议处理后再上线${NC}"
            exit 0
        fi
    else
        echo -e "${RED}✗ 有 $FAILED_CHECKS 个严重问题，必须修复后才能上线！${NC}"
        exit 1
    fi
}

# 执行主函数
main
