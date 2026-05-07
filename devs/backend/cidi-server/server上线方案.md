# CIDI Server 上线方案

## 文档说明

本方案旨在**不修改现有业务逻辑**的前提下，完成代码稳定、测试验证和生产环境部署工作。

---

## 一、当前状态评估

### 1.1 已完成的工作
- ✅ 三环境配置（dev/uat/prod）已就绪
- ✅ 微信小程序登录和手机号绑定接口已实现
- ✅ 核心业务模块已开发：
  - 用户关注（UserFollow）
  - 用户设置（UserSetting）
  - 地点小时流量（LocationHourlyFlow）
  - 拥挤指数计算
  - 预测接口
- ✅ FastAPI 建模服务（cidi-model）已独立部署并可与后端通信

### 1.2 待处理问题
- ⚠️ **15+ 个文件未提交**（Java 业务代码 + Python 模型代码）
- ⚠️ 最近多次修复"模拟上报游客数量趋势"问题，稳定性待验证
- ⚠️ 版本号为 `0.0.1-SNAPSHOT`，需更新为正式版本
- ⚠️ 缺少部署配置（Dockerfile、docker-compose.yml）
- ⚠️ 测试覆盖不足（仅有基础测试框架）
- ⚠️ 缺少监控、日志和告警配置

---

## 二、上线前修复计划（不修改业务逻辑）

### 2.1 代码稳定化（预计 2-3 天）

#### 步骤 1：提交当前所有修改
```bash
# 1. 查看所有修改
git status

# 2. 分模块提交（建议分开提交便于回滚）
# 提交 Java 后端修改
git add src/main/java/org/daiso/cidi/modules/locationhourlyflow/
git add src/main/java/org/daiso/cidi/modules/userfollow/
git add src/main/java/org/daiso/cidi/modules/usersetting/
git commit -m "fix: 完善用户关注、用户设置和地点流量模块"

# 提交 Python 模型修改
git add ../cidi-model/
git commit -m "fix(model): 修复游客数量趋势模拟逻辑"

# 3. 推送到远程
git push origin fullcircle
```

#### 步骤 2：验证编译和启动
```bash
# 清理并重新编译
mvn clean package -DskipTests

# 本地启动验证（dev 环境）
export SPRING_PROFILES_ACTIVE=dev
export WX_APPID_DEV=your_dev_appid
export WX_SECRET_DEV=your_dev_secret
export JWT_SECRET_DEV=your_dev_jwt_secret
export SDP=your_db_password

java -jar target/cidi-0.0.1-SNAPSHOT.jar

# 验证关键接口
curl http://localhost:8080/v3/api-docs
curl http://localhost:8080/doc.html
```

#### 步骤 3：修复编译和启动错误
- 如果有编译错误，仅修复语法和依赖问题
- 如果有启动错误，检查配置和环境变量
- **不修改业务逻辑**，只修复技术性错误

### 2.2 数据库迁移准备（预计 1 天）

#### 步骤 1：备份生产数据库
```bash
# 在生产服务器执行
mysqldump -h 47.111.180.134 -u root -p cidi-db > cidi-db-backup-$(date +%Y%m%d).sql
```

#### 步骤 2：检查 JPA DDL 变更
```bash
# 临时修改配置查看 DDL
# 在 application.properties 中临时设置：
# spring.jpa.hibernate.ddl-auto=validate

# 启动应用，查看是否有 schema 不匹配错误
# 如果有错误，记录需要的 DDL 变更
```

#### 步骤 3：准备数据库迁移脚本
```sql
-- migration-v0.0.1-to-v1.0.0.sql
-- 根据 JPA 实体变更生成的 DDL

-- 示例：
-- ALTER TABLE user_follow ADD COLUMN xxx VARCHAR(255);
-- CREATE INDEX idx_location_hourly_flow_record_hour ON location_hourly_flow(record_hour);
```

**注意：** 如果当前 `spring.jpa.hibernate.ddl-auto=update`，JPA 会自动更新 schema，但生产环境建议改为 `validate` 并手动执行迁移脚本。

---

## 三、测试验证计划（预计 3-5 天）

### 3.1 UAT 环境测试

#### 步骤 1：部署到 UAT 环境
```bash
# 设置 UAT 环境变量
export SPRING_PROFILES_ACTIVE=uat
export WX_APPID_UAT=your_uat_appid
export WX_SECRET_UAT=your_uat_secret
export JWT_SECRET_UAT=your_uat_jwt_secret
export SPRING_DATASOURCE_URL=jdbc:mysql://uat-db-host:3306/cidi-db-uat
export SPRING_DATASOURCE_USERNAME=root
export SDP=your_uat_db_password

# 启动服务
java -jar target/cidi-0.0.1-SNAPSHOT.jar
```

#### 步骤 2：功能测试清单

**核心功能测试：**
- [ ] 微信小程序登录（`POST /api/auth/wechat/login`）
- [ ] 手机号绑定（`POST /api/auth/wechat/phone/bind`）
- [ ] 用户关注功能（增删改查）
- [ ] 用户设置功能（增删改查）
- [ ] 地点小时流量查询（最近 N 小时、最新数据）
- [ ] 地点小时流量批量上报（来自 cidi-model）
- [ ] 拥挤指数计算接口
- [ ] 预测接口

**边界测试：**
- [ ] 无效 token 访问
- [ ] 参数校验（空值、超长、非法字符）
- [ ] 并发请求（使用 JMeter 或 ab 工具）
- [ ] 数据库连接池耗尽场景

**集成测试：**
- [ ] cidi-model（FastAPI）与 cidi-server（Spring Boot）通信
- [ ] 微信 API 调用（登录、获取手机号）
- [ ] 数据库读写一致性

#### 步骤 3：性能基准测试
```bash
# 使用 Apache Bench 进行简单压测
ab -n 1000 -c 10 -H "Authorization: Bearer YOUR_TOKEN" \
   http://uat-server:8080/api/location-hourly-flow/recent?hours=24

# 记录：
# - 平均响应时间
# - 95% 响应时间
# - 错误率
```

### 3.2 回归测试

#### 步骤 1：验证历史功能未受影响
- 使用 Postman/Swagger 回放所有已有接口
- 对比 UAT 和 DEV 环境的响应结果

#### 步骤 2：数据一致性检查
```sql
-- 检查关键表的数据完整性
SELECT COUNT(*) FROM user_follow;
SELECT COUNT(*) FROM user_setting;
SELECT COUNT(*) FROM location_hourly_flow;

-- 检查是否有异常数据
SELECT * FROM location_hourly_flow WHERE flow_count < 0 OR flow_count > 100000;
```

---

## 四、生产环境部署方案（预计 2-3 天）

### 4.1 部署前准备

#### 步骤 1：更新版本号
```xml
<!-- pom.xml -->
<version>1.0.0</version>
```

```bash
# 重新打包
mvn clean package -DskipTests
```

#### 步骤 2：准备生产环境配置
```bash
# 创建生产环境变量文件（不提交到 Git）
# prod-env.sh
export SPRING_PROFILES_ACTIVE=prod
export WX_APPID_PROD=your_prod_appid
export WX_SECRET_PROD=your_prod_secret
export JWT_SECRET_PROD=$(openssl rand -base64 32)
export SPRING_DATASOURCE_URL=jdbc:mysql://47.111.180.134:3306/cidi-db?useSSL=true&serverTimezone=UTC
export SPRING_DATASOURCE_USERNAME=root
export SDP=your_prod_db_password
export WX_MOCK_LOGIN_ENABLED=false
```

#### 步骤 3：创建 Dockerfile（可选，推荐）
```dockerfile
# Dockerfile
FROM openjdk:17-jdk-slim

WORKDIR /app

COPY target/cidi-1.0.0.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

```yaml
# docker-compose.yml
version: '3.8'

services:
  cidi-server:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - WX_APPID_PROD=${WX_APPID_PROD}
      - WX_SECRET_PROD=${WX_SECRET_PROD}
      - JWT_SECRET_PROD=${JWT_SECRET_PROD}
      - SPRING_DATASOURCE_URL=${SPRING_DATASOURCE_URL}
      - SPRING_DATASOURCE_USERNAME=${SPRING_DATASOURCE_USERNAME}
      - SDP=${SDP}
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
```

### 4.2 部署步骤

#### 方案 A：直接部署 JAR（简单快速）
```bash
# 1. 上传 JAR 到生产服务器
scp target/cidi-1.0.0.jar user@prod-server:/opt/cidi/

# 2. SSH 到生产服务器
ssh user@prod-server

# 3. 加载环境变量
source /opt/cidi/prod-env.sh

# 4. 启动服务（使用 nohup 或 systemd）
nohup java -jar /opt/cidi/cidi-1.0.0.jar > /opt/cidi/logs/app.log 2>&1 &

# 5. 验证启动
tail -f /opt/cidi/logs/app.log
curl http://localhost:8080/actuator/health
```

#### 方案 B：Docker 部署（推荐）
```bash
# 1. 构建镜像
docker build -t cidi-server:1.0.0 .

# 2. 推送到镜像仓库（可选）
docker tag cidi-server:1.0.0 your-registry/cidi-server:1.0.0
docker push your-registry/cidi-server:1.0.0

# 3. 在生产服务器启动
docker-compose up -d

# 4. 查看日志
docker-compose logs -f cidi-server
```

### 4.3 部署验证

#### 步骤 1：健康检查
```bash
# 检查服务状态
curl http://prod-server:8080/actuator/health

# 检查 API 文档
curl http://prod-server:8080/v3/api-docs

# 检查数据库连接
# 查看日志中是否有数据库连接错误
```

#### 步骤 2：烟雾测试
```bash
# 测试登录接口（使用真实微信 code）
curl -X POST http://prod-server:8080/api/auth/wechat/login \
  -H "Content-Type: application/json" \
  -d '{"code": "REAL_WECHAT_CODE"}'

# 测试需要认证的接口
curl -X GET http://prod-server:8080/api/user-follow/list \
  -H "Authorization: Bearer YOUR_TOKEN"
```

#### 步骤 3：监控指标
```bash
# CPU 和内存使用
top -p $(pgrep -f cidi-1.0.0.jar)

# JVM 堆内存
jstat -gc $(pgrep -f cidi-1.0.0.jar) 1000

# 数据库连接数
mysql -h 47.111.180.134 -u root -p -e "SHOW PROCESSLIST;"
```

---

## 五、上线后监控和应急预案

### 5.1 监控配置

#### 步骤 1：启用 Spring Boot Actuator（已配置）
```properties
# application-prod.properties 添加
management.endpoints.web.exposure.include=health,info,metrics,prometheus
management.endpoint.health.show-details=when-authorized
```

#### 步骤 2：日志监控
```bash
# 实时监控错误日志
tail -f /opt/cidi/logs/app.log | grep -i "error\|exception"

# 监控慢查询（如果启用了 SQL 日志）
tail -f /opt/cidi/logs/app.log | grep -i "slow query"
```

#### 步骤 3：告警配置（可选）
- 使用云服务商的监控告警（阿里云、腾讯云）
- 配置 CPU > 80%、内存 > 80%、磁盘 > 90% 告警
- 配置 HTTP 5xx 错误率 > 1% 告警

### 5.2 应急预案

#### 场景 1：服务无法启动
```bash
# 1. 检查日志
tail -100 /opt/cidi/logs/app.log

# 2. 常见问题：
# - 端口被占用：lsof -i :8080
# - 数据库连接失败：检查 SPRING_DATASOURCE_URL 和密码
# - 环境变量缺失：检查 prod-env.sh 是否加载

# 3. 回滚到上一个版本
cp /opt/cidi/backup/cidi-0.0.1-SNAPSHOT.jar /opt/cidi/cidi-1.0.0.jar
systemctl restart cidi-server
```

#### 场景 2：接口响应慢或超时
```bash
# 1. 检查数据库连接池
# 查看日志中是否有 "Connection pool exhausted"

# 2. 检查数据库慢查询
mysql -h 47.111.180.134 -u root -p -e "SHOW FULL PROCESSLIST;"

# 3. 临时扩容（如果使用 Docker）
docker-compose up -d --scale cidi-server=2
```

#### 场景 3：数据异常
```bash
# 1. 立即停止数据写入
# 在 cidi-model 中关闭数据上报开关

# 2. 恢复数据库备份
mysql -h 47.111.180.134 -u root -p cidi-db < cidi-db-backup-YYYYMMDD.sql

# 3. 分析异常原因
# 查看 location_hourly_flow 表的异常数据
SELECT * FROM location_hourly_flow 
WHERE record_hour > NOW() OR flow_count < 0 
ORDER BY record_hour DESC LIMIT 100;
```

#### 场景 4：微信登录失败
```bash
# 1. 检查微信 AppID 和 Secret 是否正确
echo $WX_APPID_PROD
echo $WX_SECRET_PROD

# 2. 检查微信 API 是否可达
curl https://api.weixin.qq.com/sns/jscode2session?appid=APPID&secret=SECRET&js_code=CODE&grant_type=authorization_code

# 3. 临时启用 Mock 登录（仅用于紧急调试）
export WX_MOCK_LOGIN_ENABLED=true
systemctl restart cidi-server
```

### 5.3 回滚流程

#### 快速回滚（5 分钟内）
```bash
# 1. 停止当前服务
systemctl stop cidi-server
# 或
docker-compose down

# 2. 恢复上一个版本
cp /opt/cidi/backup/cidi-0.0.1-SNAPSHOT.jar /opt/cidi/cidi-1.0.0.jar

# 3. 启动服务
systemctl start cidi-server
# 或
docker-compose up -d

# 4. 验证
curl http://prod-server:8080/actuator/health
```

#### 数据库回滚（如果有 schema 变更）
```bash
# 1. 停止应用
systemctl stop cidi-server

# 2. 恢复数据库
mysql -h 47.111.180.134 -u root -p cidi-db < cidi-db-backup-YYYYMMDD.sql

# 3. 启动旧版本应用
java -jar /opt/cidi/backup/cidi-0.0.1-SNAPSHOT.jar
```

---

## 六、上线时间表

### 第 1 周：代码稳定和测试准备
- **Day 1-2**：提交所有未提交代码，修复编译和启动错误
- **Day 3**：准备数据库迁移脚本，备份生产数据库
- **Day 4-5**：部署到 UAT 环境，执行功能测试

### 第 2 周：UAT 测试和修复
- **Day 1-3**：完成 UAT 功能测试、边界测试、集成测试
- **Day 4**：性能基准测试，记录指标
- **Day 5**：修复测试中发现的问题（仅修复 bug，不改业务逻辑）

### 第 3 周：生产部署
- **Day 1**：更新版本号，准备生产配置和部署脚本
- **Day 2**：创建 Dockerfile 和 docker-compose.yml（可选）
- **Day 3**：**生产部署日**（建议选择周三或周四，避免周五）
  - 10:00 - 12:00：部署到生产环境
  - 12:00 - 14:00：烟雾测试和监控
  - 14:00 - 18:00：观察期，处理突发问题
- **Day 4-5**：持续监控，优化配置

---

## 七、上线检查清单

### 部署前检查
- [ ] 所有代码已提交并推送到远程仓库
- [ ] 版本号已更新为 `1.0.0`
- [ ] UAT 环境测试全部通过
- [ ] 生产数据库已备份
- [ ] 生产环境变量已配置并验证
- [ ] Dockerfile 和 docker-compose.yml 已准备（如果使用 Docker）
- [ ] 回滚脚本已准备并测试
- [ ] 团队成员已通知上线时间

### 部署中检查
- [ ] 服务成功启动，无启动错误
- [ ] 健康检查接口返回正常（`/actuator/health`）
- [ ] 数据库连接正常
- [ ] 微信登录接口测试通过
- [ ] 关键业务接口测试通过

### 部署后检查
- [ ] 监控指标正常（CPU、内存、响应时间）
- [ ] 日志中无严重错误
- [ ] 数据库连接池使用正常
- [ ] 微信小程序前端可正常访问
- [ ] 用户反馈正常

---

## 八、风险评估和缓解措施

### 高风险项
| 风险 | 影响 | 概率 | 缓解措施 |
|------|------|------|----------|
| 数据库 schema 不兼容 | 高 | 中 | 提前在 UAT 验证，准备回滚脚本 |
| 微信 API 调用失败 | 高 | 低 | 验证生产 AppID/Secret，准备 Mock 登录 |
| 性能不达标 | 中 | 中 | UAT 压测，准备扩容方案 |
| 配置错误导致无法启动 | 高 | 低 | 使用配置文件模板，部署前验证 |

### 中风险项
| 风险 | 影响 | 概率 | 缓解措施 |
|------|------|------|----------|
| cidi-model 通信失败 | 中 | 低 | 验证网络连通性，准备降级方案 |
| 并发量超出预期 | 中 | 中 | 监控 QPS，准备限流和扩容 |
| 日志文件占满磁盘 | 中 | 低 | 配置日志轮转，监控磁盘使用 |

---

## 九、后续优化建议（上线后）

### 短期优化（1 个月内）
1. **完善监控**：接入 Prometheus + Grafana
2. **完善日志**：接入 ELK（Elasticsearch + Logstash + Kibana）
3. **完善测试**：补充单元测试和集成测试
4. **性能优化**：根据监控数据优化慢查询和接口

### 中期优化（3 个月内）
1. **CI/CD**：配置 GitHub Actions 或 Jenkins 自动化部署
2. **高可用**：配置负载均衡和多实例部署
3. **数据库优化**：添加索引，优化查询
4. **安全加固**：HTTPS、SQL 注入防护、XSS 防护

### 长期优化（6 个月内）
1. **微服务拆分**：根据业务增长拆分服务
2. **缓存层**：引入 Redis 缓存热点数据
3. **消息队列**：引入 RabbitMQ 或 Kafka 处理异步任务
4. **容器编排**：迁移到 Kubernetes

---

## 十、联系人和资源

### 技术负责人
- **后端负责人**：[姓名]
- **模型负责人**：[姓名]
- **前端负责人**：[姓名]

### 关键资源
- **代码仓库**：[Git 仓库地址]
- **UAT 环境**：[UAT 服务器地址]
- **生产环境**：[生产服务器地址]
- **数据库**：47.111.180.134:3306
- **API 文档**：http://prod-server:8080/doc.html

### 应急联系
- **运维支持**：[电话/微信]
- **数据库管理员**：[电话/微信]
- **产品经理**：[电话/微信]

---

## 附录

### A. 常用命令速查

```bash
# 查看服务状态
systemctl status cidi-server
docker-compose ps

# 查看日志
tail -f /opt/cidi/logs/app.log
docker-compose logs -f cidi-server

# 重启服务
systemctl restart cidi-server
docker-compose restart cidi-server

# 查看 JVM 内存
jstat -gc $(pgrep -f cidi-1.0.0.jar)

# 查看数据库连接
mysql -h 47.111.180.134 -u root -p -e "SHOW PROCESSLIST;"

# 备份数据库
mysqldump -h 47.111.180.134 -u root -p cidi-db > backup-$(date +%Y%m%d-%H%M%S).sql
```

### B. 配置文件模板

见 `src/main/resources/application-prod.properties`

### C. 测试用例模板

见 `src/test/java/` 目录

---

**文档版本**：v1.0  
**最后更新**：2026-05-07  
**维护人**：[姓名]
