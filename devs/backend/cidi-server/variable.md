# CIDI Server 环境变量配置文档

## 文档说明

本文档详细说明 CIDI Server 部署所需的所有环境变量、配置项及快速部署方案。

---

## 一、必需环境变量清单

### 1.1 核心配置（所有环境必填）

#### 数据库配置
```bash
# 数据库密码（必填）
SDP=your_database_password

# 数据库连接（可选，有默认值）
SPRING_DATASOURCE_URL=jdbc:mysql://47.111.180.134:3306/cidi-db?useSSL=false&serverTimezone=UTC
SPRING_DATASOURCE_USERNAME=root
```

**说明**：
- `SDP`：数据库密码，**必须配置**，无默认值
- `SPRING_DATASOURCE_URL`：默认值已配置为 `47.111.180.134:3306/cidi-db`
- `SPRING_DATASOURCE_USERNAME`：默认值为 `root`

#### 环境选择
```bash
# 运行环境：dev / uat / prod（必填）
SPRING_PROFILES_ACTIVE=dev
```

**说明**：
- `dev`：开发环境，启用 Mock 登录，使用默认 JWT 密钥
- `uat`：UAT 测试环境，需配置真实微信凭证
- `prod`：生产环境，需配置所有安全凭证

---

### 1.2 微信小程序配置（按环境）

#### 开发环境（dev）
```bash
WX_APPID_DEV=your_wechat_appid
WX_SECRET_DEV=your_wechat_secret
WX_MOCK_LOGIN_ENABLED=true  # 可选，默认 true
```

#### UAT 环境（uat）
```bash
WX_APPID_UAT=your_wechat_appid
WX_SECRET_UAT=your_wechat_secret
WX_MOCK_LOGIN_ENABLED=false  # 可选，默认 false
```

#### 生产环境（prod）
```bash
WX_APPID_PROD=your_wechat_appid
WX_SECRET_PROD=your_wechat_secret
WX_MOCK_LOGIN_ENABLED=false  # 可选，默认 false
```

**说明**：
- 如果启用 `WX_MOCK_LOGIN_ENABLED=true`，可以跳过微信 AppID/Secret 配置
- Mock 模式下，任意字符串都可作为登录 code，系统会生成 `mock_xxx` 格式的 openid

---

### 1.3 JWT 配置（按环境）

#### 开发环境（dev）
```bash
# 可选，有默认值
JWT_SECRET_DEV=dev-jwt-secret-dev-jwt-secret-dev-jwt-secret-123456
```

#### UAT/生产环境（uat/prod）
```bash
# 必填，无默认值
JWT_SECRET_UAT=your_jwt_secret_here
JWT_SECRET_PROD=your_jwt_secret_here
```

**生成强密钥**：
```bash
# 使用 openssl 生成 32 字节随机密钥
openssl rand -base64 32
```

---

## 二、快速部署方案

### 方案 1：最小配置（Mock 登录模式）⭐ 推荐测试

**适用场景**：内部测试、快速验证、不需要真实微信登录

#### 配置文件：`quick-start.sh`
```bash
#!/bin/bash
# CIDI Server 快速启动脚本（Mock 登录模式）

# 环境选择
export SPRING_PROFILES_ACTIVE=dev

# 数据库密码（唯一必填项）
export SDP="YOUR_DB_PASSWORD_HERE"

# 启用 Mock 登录（跳过微信验证）
export WX_MOCK_LOGIN_ENABLED=true

# 启动服务
echo "正在启动 CIDI Server（Mock 登录模式）..."
java -jar target/cidi-0.0.1-SNAPSHOT.jar
```

#### 使用方法
```bash
# 1. 打包项目
./mvnw clean package -DskipTests

# 2. 修改脚本中的数据库密码
vim quick-start.sh

# 3. 启动服务
chmod +x quick-start.sh
./quick-start.sh
```

#### 优点
- ✅ 只需配置 1 个环境变量（`SDP`）
- ✅ 不需要微信 AppID/Secret
- ✅ 任意字符串可作为登录 code
- ✅ 适合快速验证业务逻辑

---

### 方案 2：完整配置（生产模式）

**适用场景**：UAT 测试、生产部署、需要真实微信登录

#### 配置文件：`prod-env.sh`
```bash
#!/bin/bash
# CIDI Server 生产环境配置

# 环境选择
export SPRING_PROFILES_ACTIVE=prod

# 数据库配置
export SPRING_DATASOURCE_URL=jdbc:mysql://47.111.180.134:3306/cidi-db?useSSL=true&serverTimezone=UTC
export SPRING_DATASOURCE_USERNAME=root
export SDP="YOUR_PROD_DB_PASSWORD"

# 微信小程序配置
export WX_APPID_PROD="wx1234567890abcdef"
export WX_SECRET_PROD="abcdef1234567890abcdef1234567890"
export WX_MOCK_LOGIN_ENABLED=false

# JWT 密钥（使用 openssl rand -base64 32 生成）
export JWT_SECRET_PROD="YOUR_GENERATED_JWT_SECRET"

# 启动服务
echo "正在启动 CIDI Server（生产模式）..."
java -jar target/cidi-1.0.0.jar
```

#### 使用方法
```bash
# 1. 创建配置文件（不要提交到 Git）
cp prod-env.sh.template prod-env.sh
vim prod-env.sh  # 填入真实配置

# 2. 加载环境变量并启动
source prod-env.sh
```

---

## 三、环境变量配置矩阵

| 环境变量 | dev 环境 | uat 环境 | prod 环境 | 默认值 | 说明 |
|---------|---------|---------|----------|--------|------|
| `SPRING_PROFILES_ACTIVE` | `dev` | `uat` | `prod` | `dev` | 环境选择 |
| `SDP` | ✅ 必填 | ✅ 必填 | ✅ 必填 | 无 | 数据库密码 |
| `SPRING_DATASOURCE_URL` | 可选 | 可选 | 可选 | `jdbc:mysql://47.111.180.134:3306/cidi-db` | 数据库连接 |
| `SPRING_DATASOURCE_USERNAME` | 可选 | 可选 | 可选 | `root` | 数据库用户名 |
| `WX_APPID_*` | 可选 | ✅ 必填 | ✅ 必填 | 无 | 微信 AppID |
| `WX_SECRET_*` | 可选 | ✅ 必填 | ✅ 必填 | 无 | 微信 Secret |
| `WX_MOCK_LOGIN_ENABLED` | 可选 | 可选 | 可选 | dev: `true`<br>uat/prod: `false` | Mock 登录开关 |
| `JWT_SECRET_*` | 可选 | ✅ 必填 | ✅ 必填 | dev 有默认值 | JWT 签名密钥 |

---

## 四、配置文件说明

### 4.1 配置文件位置
```
src/main/resources/
├── application.properties          # 基础配置
├── application-dev.properties      # 开发环境
├── application-uat.properties      # UAT 环境
└── application-prod.properties     # 生产环境
```

### 4.2 配置优先级
1. **环境变量**（最高优先级）
2. **application-{profile}.properties**
3. **application.properties**（最低优先级）

### 4.3 配置示例

#### application.properties（基础配置）
```properties
spring.application.name=cidi
spring.profiles.active=${SPRING_PROFILES_ACTIVE:dev}

# 数据库配置
spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:mysql://47.111.180.134:3306/cidi-db?useSSL=false&serverTimezone=UTC}
spring.datasource.username=${SPRING_DATASOURCE_USERNAME:root}
spring.datasource.password=${SDP}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA 配置
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# 服务器配置
server.port=8080
server.address=0.0.0.0
```

#### application-dev.properties（开发环境）
```properties
# 微信配置
wx.appid=${WX_APPID_DEV:${WX_APPID:}}
wx.secret=${WX_SECRET_DEV:${WX_SECRET:}}
wx.mini-program-env=dev
wx.mock-login-enabled=${WX_MOCK_LOGIN_ENABLED:true}

# JWT 配置（有默认值）
jwt.secret=${JWT_SECRET_DEV:${JWT_SECRET:dev-jwt-secret-dev-jwt-secret-dev-jwt-secret-123456}}
```

#### application-prod.properties（生产环境）
```properties
# 微信配置
wx.appid=${WX_APPID_PROD:${WX_APPID:}}
wx.secret=${WX_SECRET_PROD:${WX_SECRET:}}
wx.mini-program-env=release
wx.mock-login-enabled=${WX_MOCK_LOGIN_ENABLED:false}

# JWT 配置（无默认值，必须配置）
jwt.secret=${JWT_SECRET_PROD:${JWT_SECRET:}}
```

---

## 五、部署步骤

### 5.1 本地开发部署

```bash
# 1. 打包项目
./mvnw clean package -DskipTests

# 2. 配置环境变量（最小配置）
export SPRING_PROFILES_ACTIVE=dev
export SDP="your_db_password"
export WX_MOCK_LOGIN_ENABLED=true

# 3. 启动服务
java -jar target/cidi-0.0.1-SNAPSHOT.jar

# 4. 验证服务
curl http://localhost:8080/v3/api-docs
curl http://localhost:8080/doc.html
```

### 5.2 服务器部署（JAR 方式）

```bash
# 1. 打包项目
./mvnw clean package -DskipTests

# 2. 上传到服务器
scp target/cidi-0.0.1-SNAPSHOT.jar user@server:/opt/cidi/

# 3. SSH 到服务器
ssh user@server

# 4. 创建环境变量文件
cat > /opt/cidi/env.sh << 'EOF'
export SPRING_PROFILES_ACTIVE=prod
export SDP="your_prod_db_password"
export WX_APPID_PROD="wx1234567890abcdef"
export WX_SECRET_PROD="abcdef1234567890abcdef1234567890"
export JWT_SECRET_PROD="your_jwt_secret"
EOF

# 5. 启动服务
source /opt/cidi/env.sh
nohup java -jar /opt/cidi/cidi-0.0.1-SNAPSHOT.jar > /opt/cidi/logs/app.log 2>&1 &

# 6. 查看日志
tail -f /opt/cidi/logs/app.log
```

### 5.3 Docker 部署

#### 创建 Dockerfile
```dockerfile
FROM openjdk:17-jdk-slim

WORKDIR /app

COPY target/cidi-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### 创建 docker-compose.yml
```yaml
version: '3.8'

services:
  cidi-server:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE:-prod}
      - SDP=${SDP}
      - WX_APPID_PROD=${WX_APPID_PROD}
      - WX_SECRET_PROD=${WX_SECRET_PROD}
      - JWT_SECRET_PROD=${JWT_SECRET_PROD}
      - SPRING_DATASOURCE_URL=${SPRING_DATASOURCE_URL:-jdbc:mysql://47.111.180.134:3306/cidi-db?useSSL=false&serverTimezone=UTC}
      - SPRING_DATASOURCE_USERNAME=${SPRING_DATASOURCE_USERNAME:-root}
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
```

#### 部署命令
```bash
# 1. 构建镜像
docker build -t cidi-server:latest .

# 2. 启动服务（使用 .env 文件）
cat > .env << 'EOF'
SPRING_PROFILES_ACTIVE=prod
SDP=your_db_password
WX_APPID_PROD=wx1234567890abcdef
WX_SECRET_PROD=abcdef1234567890abcdef1234567890
JWT_SECRET_PROD=your_jwt_secret
EOF

docker-compose up -d

# 3. 查看日志
docker-compose logs -f cidi-server
```

---

## 六、验证和测试

### 6.1 健康检查
```bash
# 检查服务状态
curl http://localhost:8080/actuator/health

# 预期响应
{"status":"UP"}
```

### 6.2 API 文档访问
```bash
# Swagger UI
http://localhost:8080/doc.html

# OpenAPI JSON
http://localhost:8080/v3/api-docs
```

### 6.3 功能测试

#### Mock 登录测试（dev 环境）
```bash
# 登录接口
curl -X POST http://localhost:8080/api/auth/wechat/login \
  -H "Content-Type: application/json" \
  -d '{"code": "test123"}'

# 预期响应
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "openid": "mock_test123"
}
```

#### 真实微信登录测试（uat/prod 环境）
```bash
# 需要从微信小程序获取真实 code
curl -X POST http://localhost:8080/api/auth/wechat/login \
  -H "Content-Type: application/json" \
  -d '{"code": "REAL_WECHAT_CODE"}'
```

---

## 七、故障排查

### 7.1 服务无法启动

#### 问题：端口被占用
```bash
# 检查端口占用
lsof -i :8080

# 解决方案：杀死占用进程或修改端口
export SERVER_PORT=8081
```

#### 问题：数据库连接失败
```bash
# 检查数据库连接
mysql -h 47.111.180.134 -u root -p

# 检查环境变量
echo $SDP
echo $SPRING_DATASOURCE_URL

# 查看日志
tail -100 logs/app.log | grep -i "database\|connection"
```

#### 问题：环境变量未加载
```bash
# 检查环境变量
env | grep -E "SPRING_|WX_|JWT_|SDP"

# 重新加载
source env.sh
```

### 7.2 微信登录失败

#### 问题：AppID/Secret 配置错误
```bash
# 检查配置
echo $WX_APPID_PROD
echo $WX_SECRET_PROD

# 测试微信 API
curl "https://api.weixin.qq.com/sns/jscode2session?appid=YOUR_APPID&secret=YOUR_SECRET&js_code=TEST&grant_type=authorization_code"
```

#### 问题：启用 Mock 登录
```bash
# 临时启用 Mock 登录（仅用于调试）
export WX_MOCK_LOGIN_ENABLED=true
# 重启服务
```

### 7.3 JWT 验证失败

#### 问题：JWT 密钥未配置
```bash
# 检查 JWT 密钥
echo $JWT_SECRET_PROD

# 生成新密钥
openssl rand -base64 32
```

---

## 八、安全建议

### 8.1 环境变量管理

⚠️ **不要将敏感信息提交到 Git**

```bash
# 添加到 .gitignore
echo "*.env" >> .gitignore
echo "*-env.sh" >> .gitignore
echo "prod-*.sh" >> .gitignore
```

### 8.2 密钥强度要求

- **JWT_SECRET**：至少 32 字节，使用 `openssl rand -base64 32` 生成
- **数据库密码**：至少 16 字符，包含大小写字母、数字、特殊字符
- **微信 Secret**：由微信平台提供，不要泄露

### 8.3 生产环境配置检查清单

- [ ] `WX_MOCK_LOGIN_ENABLED=false`（禁用 Mock 登录）
- [ ] `spring.jpa.show-sql=false`（关闭 SQL 日志）
- [ ] 使用强 JWT 密钥
- [ ] 数据库使用 SSL 连接（`useSSL=true`）
- [ ] 配置防火墙规则，限制数据库访问
- [ ] 定期备份数据库

---

## 九、常见问题 FAQ

### Q1: 只配置 SDP 能启动吗？
**A**: 可以。在 dev 环境下，启用 `WX_MOCK_LOGIN_ENABLED=true`，只需配置 `SDP` 即可启动。

### Q2: Mock 登录模式下如何测试？
**A**: 使用任意字符串作为 code，系统会生成 `mock_xxx` 格式的 openid。例如：
```bash
curl -X POST http://localhost:8080/api/auth/wechat/login \
  -H "Content-Type: application/json" \
  -d '{"code": "testuser"}'
# 返回 openid: "mock_testuser"
```

### Q3: 如何切换环境？
**A**: 修改 `SPRING_PROFILES_ACTIVE` 环境变量：
```bash
export SPRING_PROFILES_ACTIVE=dev   # 开发环境
export SPRING_PROFILES_ACTIVE=uat   # UAT 环境
export SPRING_PROFILES_ACTIVE=prod  # 生产环境
```

### Q4: 数据库地址如何修改？
**A**: 设置 `SPRING_DATASOURCE_URL` 环境变量：
```bash
export SPRING_DATASOURCE_URL=jdbc:mysql://your-db-host:3306/your-db-name?useSSL=false&serverTimezone=UTC
```

### Q5: 如何查看当前配置？
**A**: 访问 Spring Boot Actuator 端点（需要配置）：
```bash
curl http://localhost:8080/actuator/env
```

---

## 十、快速参考

### 最小启动配置（测试）
```bash
export SPRING_PROFILES_ACTIVE=dev
export SDP="your_db_password"
export WX_MOCK_LOGIN_ENABLED=true
java -jar target/cidi-0.0.1-SNAPSHOT.jar
```

### 完整启动配置（生产）
```bash
export SPRING_PROFILES_ACTIVE=prod
export SDP="your_prod_db_password"
export WX_APPID_PROD="wx1234567890abcdef"
export WX_SECRET_PROD="abcdef1234567890abcdef1234567890"
export JWT_SECRET_PROD="$(openssl rand -base64 32)"
export WX_MOCK_LOGIN_ENABLED=false
java -jar target/cidi-1.0.0.jar
```

### 一键部署脚本
```bash
#!/bin/bash
# deploy.sh - 一键部署脚本

set -e

echo "=== CIDI Server 部署脚本 ==="

# 1. 打包
echo "[1/4] 打包项目..."
./mvnw clean package -DskipTests

# 2. 配置环境变量
echo "[2/4] 配置环境变量..."
if [ ! -f ".env" ]; then
    echo "错误：.env 文件不存在，请先创建配置文件"
    exit 1
fi
source .env

# 3. 验证必需变量
echo "[3/4] 验证配置..."
if [ -z "$SDP" ]; then
    echo "错误：SDP 环境变量未设置"
    exit 1
fi

# 4. 启动服务
echo "[4/4] 启动服务..."
java -jar target/cidi-0.0.1-SNAPSHOT.jar

echo "✅ 部署完成"
```

---

**文档版本**：v2.0  
**最后更新**：2026-05-09  
**维护人**：CIDI Team

