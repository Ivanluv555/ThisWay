# 项目上线检查清单

本文档提供全面的生产环境部署检查项和自动化验证方法。

---

## 一、代码质量检查

### 1.1 代码规范
- [ ] 移除所有 `TODO`、`FIXME`、`XXX`、`HACK` 注释或转为 Issue
- [ ] 移除调试代码（`console.log`、`System.out.println`、`print()`）
- [ ] 移除注释掉的代码块
- [ ] 代码格式化统一（Java: Google Style / Alibaba Style，Python: PEP8）

**验证方法：**
```bash
# 检查 TODO/FIXME
grep -r "TODO\|FIXME\|XXX\|HACK" devs/backend/cidi-server/src/main/java --include="*.java"
grep -r "TODO\|FIXME\|XXX\|HACK" devs/backend/cidi-model --include="*.py"

# 检查调试输出
grep -r "System.out.println\|printStackTrace" devs/backend/cidi-server/src/main/java --include="*.java"
grep -r "console.log\|console.error" devs/frontend/cidi-wx --include="*.js"
grep -r "print(" devs/backend/cidi-model --include="*.py" | grep -v "# print"
```

### 1.2 代码审查
- [ ] 所有功能分支已合并到主分支
- [ ] 代码已通过团队 Code Review
- [ ] 关键业务逻辑有注释说明
- [ ] 复杂算法有文档说明

---

## 二、安全检查

### 2.1 敏感信息
- [ ] **关键** 配置文件中无硬编码密码、密钥、Token
- [ ] **关键** `.env` 文件已加入 `.gitignore`
- [ ] **关键** 数据库密码通过环境变量注入
- [ ] **关键** 微信 AppID/Secret 通过环境变量注入
- [ ] **关键** JWT Secret 使用强密钥（至少 32 字符）
- [ ] API 密钥、第三方服务凭证已移除

**验证方法：**
```bash
# 检查硬编码密码
grep -r "password\s*=\s*['\"]" devs/backend --include="*.properties" --include="*.yml" --include="*.py"
grep -r "secret\s*=\s*['\"]" devs/backend --include="*.properties" --include="*.yml"

# 检查是否有敏感文件被提交
git log --all --full-history -- "*.env" "*.pem" "*.key" "*secret*" "*password*"

# 检查配置文件中的 IP 地址和域名
grep -r "47.111.180.134" devs/backend/cidi-server/src/main/resources/
```

### 2.2 认证授权
- [ ] 所有需要登录的接口已添加鉴权
- [ ] JWT Token 过期时间合理（建议 7 天）
- [ ] 敏感操作需要二次验证
- [ ] 防止越权访问（用户只能访问自己的数据）
- [ ] 管理员接口有权限控制

**验证方法：**
```bash
# 检查 Controller 是否有鉴权注解
find devs/backend/cidi-server/src/main/java -name "*Controller.java" -exec grep -L "@PreAuthorize\|@Secured\|requireAuth" {} \;
```

### 2.3 输入验证
- [ ] 所有用户输入已进行验证（`@Valid`、`@NotNull` 等）
- [ ] SQL 查询使用参数化查询，防止 SQL 注入
- [ ] 文件上传有类型、大小限制
- [ ] XSS 防护（前端输出转义）
- [ ] CSRF 防护（POST/PUT/DELETE 接口）

**验证方法：**
```bash
# 检查是否有拼接 SQL
grep -r "\"SELECT.*+\|\"INSERT.*+\|\"UPDATE.*+\|\"DELETE.*+" devs/backend/cidi-server/src/main/java --include="*.java"

# 检查 DTO 是否有验证注解
find devs/backend/cidi-server/src/main/java -path "*/dto/*" -name "*.java" -exec grep -L "@Valid\|@NotNull\|@NotBlank" {} \;
```

### 2.4 依赖安全
- [ ] 所有依赖库已更新到最新稳定版本
- [ ] 无已知安全漏洞的依赖

**验证方法：**
```bash
# Java 依赖安全检查
cd devs/backend/cidi-server
./mvnw dependency-check:check

# Python 依赖安全检查
cd devs/backend/cidi-model
pip install safety
safety check -r requirements.txt
```

---

## 三、配置检查

### 3.1 环境配置
- [ ] **关键** `application.properties` 使用环境变量占位符
- [ ] **关键** 生产环境配置文件 `application-prod.properties` 已完善
- [ ] **关键** 数据库连接池配置合理（最大连接数、超时时间）
- [ ] **关键** 日志级别设置为 `INFO` 或 `WARN`（生产环境禁用 `DEBUG`）
- [ ] **关键** `spring.jpa.show-sql=false`（生产环境）
- [ ] **关键** `spring.jpa.hibernate.ddl-auto=validate`（生产环境禁用 `update`）

**验证方法：**
```bash
# 检查生产配置
cat devs/backend/cidi-server/src/main/resources/application-prod.properties

# 检查是否有硬编码配置
grep -v "^\s*#\|^\s*$" devs/backend/cidi-server/src/main/resources/application.properties | grep -v "\${" | grep "="
```

### 3.2 前端配置
- [ ] **关键** `apiBaseUrl` 指向生产服务器地址
- [ ] **关键** 微信小程序合法域名已配置
- [ ] **关键** 微信小程序业务域名已配置
- [ ] 小程序 AppID 已切换为正式版
- [ ] 图片、文件上传路径正确

**验证方法：**
```bash
# 检查前端 API 地址
grep -r "apiBaseUrl\|baseURL" devs/frontend/cidi-wx --include="*.js"
grep "localhost\|127.0.0.1" devs/frontend/cidi-wx/app.js
```

### 3.3 数据库配置
- [ ] 数据库已创建并初始化
- [ ] 数据库字符集为 `utf8mb4`
- [ ] 数据库索引已优化
- [ ] 数据库备份策略已配置
- [ ] 数据库连接白名单已配置

**验证方法：**
```bash
# 检查数据库初始化脚本
mysql -h <host> -u <user> -p<password> -e "SHOW DATABASES LIKE 'cidi%';"
mysql -h <host> -u <user> -p<password> cidi-db -e "SHOW TABLES;"
```

---

## 四、功能测试

### 4.1 核心功能测试
- [ ] 用户注册/登录流程
- [ ] 景区信息查询
- [ ] 地图展示与交互
- [ ] 路线规划功能
- [ ] 拥挤度预测功能
- [ ] 用户关注/收藏功能
- [ ] 评论/互动功能
- [ ] 文件上传功能

### 4.2 接口测试
- [ ] 所有 API 接口返回正确状态码
- [ ] 错误处理返回友好提示
- [ ] 接口响应时间在可接受范围（< 2s）
- [ ] 并发测试通过（模拟 100+ 用户）

**验证方法：**
```bash
# 使用脚本测试核心接口
bash devs/script/test_api_health.sh

# 压力测试（需要安装 ab 或 wrk）
ab -n 1000 -c 100 http://<server-ip>:8080/api/locations
```

### 4.3 边界测试
- [ ] 空数据处理
- [ ] 超长输入处理
- [ ] 特殊字符处理
- [ ] 网络异常处理
- [ ] 并发冲突处理

---

## 五、性能优化

### 5.1 后端性能
- [ ] 数据库查询已优化（避免 N+1 查询）
- [ ] 热点数据已缓存（Redis）
- [ ] 大数据量接口已分页
- [ ] 静态资源使用 CDN
- [ ] 接口响应时间 < 2s

**验证方法：**
```bash
# 检查慢查询
grep "Hibernate.*took.*ms" devs/backend/cidi-server/logs/*.log | awk '{if($NF>1000)print}'

# 检查是否有分页
grep -r "Pageable\|PageRequest" devs/backend/cidi-server/src/main/java --include="*.java"
```

### 5.2 前端性能
- [ ] 图片已压缩优化
- [ ] 列表使用虚拟滚动或分页加载
- [ ] 避免频繁请求（防抖/节流）
- [ ] 小程序包大小 < 2MB

**验证方法：**
```bash
# 检查小程序包大小
du -sh devs/frontend/cidi-wx/
find devs/frontend/cidi-wx/images -type f -size +500k
```

---

## 六、监控与日志

### 6.1 日志配置
- [ ] **关键** 日志文件路径已配置
- [ ] **关键** 日志滚动策略已配置（按日期或大小）
- [ ] **关键** 错误日志单独输出
- [ ] 关键操作有审计日志
- [ ] 日志不包含敏感信息（密码、Token）

**验证方法：**
```bash
# 检查日志配置
ls -lh devs/backend/cidi-server/src/main/resources/ | grep log

# 检查日志中是否有敏感信息
grep -r "password\|secret\|token" devs/backend/cidi-server/logs/ | head -10
```

### 6.2 监控告警
- [ ] 服务健康检查接口（`/actuator/health`）
- [ ] 数据库连接监控
- [ ] 内存/CPU 使用率监控
- [ ] 接口错误率监控
- [ ] 关键业务指标监控

---

## 七、部署准备

### 7.1 构建打包
- [ ] **关键** Java 后端打包成功（`mvn clean package`）
- [ ] **关键** Python 模型依赖已导出（`requirements.txt`）
- [ ] **关键** 前端小程序已上传审核
- [ ] 构建产物大小合理
- [ ] 无编译警告

**验证方法：**
```bash
# Java 打包
cd devs/backend/cidi-server
./mvnw clean package -DskipTests
ls -lh target/*.jar

# Python 依赖检查
cd devs/backend/cidi-model
pip install -r requirements.txt --dry-run
```

### 7.2 服务器环境
- [ ] **关键** Java 17+ 已安装
- [ ] **关键** Python 3.9+ 已安装
- [ ] **关键** MySQL 8.0+ 已安装并配置
- [ ] **关键** Redis 已安装并配置
- [ ] 防火墙已配置（开放 8080 端口）
- [ ] 域名已解析（如需要）
- [ ] SSL 证书已配置（HTTPS）

**验证方法：**
```bash
# 服务器环境检查
ssh user@server << 'EOF'
java -version
python3 --version
mysql --version
redis-cli --version
netstat -tuln | grep 8080
EOF
```

### 7.3 部署脚本
- [ ] 自动化部署脚本已准备
- [ ] 数据库迁移脚本已准备
- [ ] 回滚方案已准备
- [ ] 启动/停止脚本已准备

---

## 八、文档完善

### 8.1 技术文档
- [ ] API 接口文档完整（Swagger/OpenAPI）
- [ ] 数据库设计文档
- [ ] 部署文档（DEPLOY.md）
- [ ] 运维手册
- [ ] 故障排查手册

### 8.2 用户文档
- [ ] 用户使用手册
- [ ] 常见问题 FAQ
- [ ] 隐私政策
- [ ] 用户协议

---

## 九、合规检查

### 9.1 法律合规
- [ ] 用户隐私政策已完善
- [ ] 用户协议已完善
- [ ] 数据收集已告知用户
- [ ] 第三方服务（微信）已备案

### 9.2 小程序审核
- [ ] 小程序类目正确
- [ ] 小程序功能描述准确
- [ ] 无违规内容
- [ ] 测试账号已提供

---

## 十、上线流程

### 10.1 上线前
- [ ] 完成所有上述检查项
- [ ] 在测试环境完整验证
- [ ] 数据库备份
- [ ] 制定上线计划（时间、人员、步骤）
- [ ] 准备回滚方案

### 10.2 上线中
- [ ] 按计划执行部署
- [ ] 实时监控服务状态
- [ ] 验证核心功能
- [ ] 记录上线日志

### 10.3 上线后
- [ ] 持续监控 24 小时
- [ ] 收集用户反馈
- [ ] 修复紧急问题
- [ ] 总结上线经验

---

## 自动化检查脚本

见 `devs/script/pre_deploy_check.sh`
