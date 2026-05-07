# 项目上线快速参考

## 一、上线前检查（5 分钟）

```bash
# 1. 运行自动化检查
bash devs/script/pre_deploy_check.sh

# 2. 确认所有严重问题（红色 ✗）已修复
# 3. 处理警告项（黄色 ⚠）
```

---

## 二、环境变量配置（必需）

```bash
# 生产环境必须配置以下环境变量：
export SPRING_PROFILES_ACTIVE=prod
export SPRING_DATASOURCE_URL='jdbc:mysql://<host>:3306/cidi-db?useSSL=false&serverTimezone=UTC'
export SPRING_DATASOURCE_USERNAME='<db-user>'
export SPRING_DATASOURCE_PASSWORD='<db-password>'
export WX_APPID_PROD='<wechat-appid>'
export WX_SECRET_PROD='<wechat-secret>'
export JWT_SECRET_PROD='<strong-jwt-secret-32-chars-min>'
```

---

## 三、一键部署（推荐）

```bash
# 1. 加载环境变量
source /opt/cidi/env.sh

# 2. 执行部署
bash devs/script/deploy.sh prod

# 3. 等待健康检查完成
# 4. 验证服务
curl http://localhost:8080/api/posts
```

---

## 四、手动部署

```bash
# 1. 构建
cd devs/backend/cidi-server
./mvnw clean package -DskipTests

# 2. 上传
scp target/cidi-0.0.1-SNAPSHOT.jar user@server:/opt/cidi/

# 3. 停止旧服务
ssh user@server
kill $(cat /opt/cidi/logs/app.pid)

# 4. 启动新服务
cd /opt/cidi
source env.sh
nohup java -jar cidi-0.0.1-SNAPSHOT.jar > logs/app.log 2>&1 &
echo $! > logs/app.pid

# 5. 验证
sleep 10
curl http://localhost:8080/api/posts
```

---

## 五、健康检查

```bash
# 运行健康检查脚本
bash devs/script/test_api_health.sh http://your-server:8080

# 手动检查
curl http://your-server:8080/api/posts
curl http://your-server:8080/api/locations
curl http://your-server:8080/doc.html
```

---

## 六、常用命令

```bash
# 查看日志
tail -f /opt/cidi/logs/app.log

# 查看进程
ps aux | grep cidi.*jar

# 停止服务
kill $(cat /opt/cidi/logs/app.pid)

# 重启服务
bash devs/script/deploy.sh prod

# 查看端口
netstat -tuln | grep 8080
```

---

## 七、故障排查

### 服务启动失败
```bash
# 1. 查看日志
tail -100 /opt/cidi/logs/app.log

# 2. 检查环境变量
echo $SPRING_PROFILES_ACTIVE
echo $SPRING_DATASOURCE_URL

# 3. 检查端口占用
netstat -tuln | grep 8080

# 4. 检查 Java 版本
java -version  # 必须 17+
```

### 数据库连接失败
```bash
# 1. 检查 MySQL 状态
sudo systemctl status mysql

# 2. 测试连接
mysql -h <host> -u <user> -p

# 3. 检查防火墙
sudo ufw status
sudo ufw allow 3306
```

### 前端无法连接
```bash
# 1. 检查防火墙
sudo ufw allow 8080

# 2. 检查云服务器安全组
# 在云服务商控制台开放 8080 端口

# 3. 测试外网访问
curl http://<public-ip>:8080/api/posts

# 4. 检查前端配置
# devs/frontend/cidi-wx/app.js
# apiBaseUrl 不能是 localhost
```

---

## 八、回滚方案

```bash
# 1. 停止服务
sudo systemctl stop cidi

# 2. 恢复旧版本
cd /opt/cidi
mv cidi-0.0.1-SNAPSHOT.jar cidi-new.jar
mv cidi-old.jar cidi-0.0.1-SNAPSHOT.jar

# 3. 启动服务
sudo systemctl start cidi

# 4. 恢复数据库（如需要）
mysql -u cidi_user -p cidi-db < backups/cidi_db_backup.sql
```

---

## 九、核心检查清单

### 安全检查（必须）
- [ ] 无硬编码密码、密钥
- [ ] 所有敏感配置使用环境变量
- [ ] JWT Secret 至少 32 字符
- [ ] 数据库密码强度足够
- [ ] 无敏感文件提交到 Git

### 配置检查（必须）
- [ ] `spring.jpa.show-sql=false` (生产环境)
- [ ] `spring.jpa.hibernate.ddl-auto=validate` (生产环境)
- [ ] 前端 `apiBaseUrl` 指向生产服务器
- [ ] 微信小程序合法域名已配置
- [ ] 数据库连接池配置合理

### 功能检查（必须）
- [ ] 用户登录功能正常
- [ ] 景区信息查询正常
- [ ] 地图展示正常
- [ ] 路线规划功能正常
- [ ] 文件上传功能正常

### 性能检查（建议）
- [ ] 接口响应时间 < 2s
- [ ] 大数据量接口已分页
- [ ] 热点数据已缓存
- [ ] 图片已压缩优化

---

## 十、联系方式

- **技术文档**: [docs/DEPLOYMENT_GUIDE.md](./DEPLOYMENT_GUIDE.md)
- **完整检查清单**: [docs/PRODUCTION_CHECKLIST.md](./PRODUCTION_CHECKLIST.md)
- **API 文档**: http://your-server:8080/doc.html
- **紧急联系**: [填写联系方式]

---

## 十一、部署时间建议

- **最佳时间**: 凌晨 2:00 - 5:00（用户访问量最低）
- **避免时间**: 节假日、周末、工作日高峰期
- **预留时间**: 至少 2 小时（包括验证和回滚时间）
- **人员要求**: 至少 2 人（1 人操作，1 人监控）

---

## 十二、上线后监控（24 小时）

```bash
# 持续监控日志
tail -f /opt/cidi/logs/app.log | grep -i error

# 监控系统资源
top -p $(cat /opt/cidi/logs/app.pid)

# 监控接口响应
watch -n 60 'curl -s -w "\nTime: %{time_total}s\n" http://localhost:8080/api/posts'

# 监控数据库连接
mysql -u cidi_user -p -e "SHOW PROCESSLIST;"
```

---

**记住：部署前必须运行 `pre_deploy_check.sh`，确保所有严重问题已修复！**
