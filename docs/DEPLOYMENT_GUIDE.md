# 项目部署指南

本文档提供完整的生产环境部署流程和自动化工具使用说明。

---

## 目录

1. [部署前准备](#部署前准备)
2. [自动化部署](#自动化部署)
3. [手动部署](#手动部署)
4. [部署验证](#部署验证)
5. [常见问题](#常见问题)
6. [回滚方案](#回滚方案)

---

## 部署前准备

### 1. 服务器环境要求

**硬件要求：**
- CPU: 2 核及以上
- 内存: 4GB 及以上
- 磁盘: 20GB 及以上

**软件要求：**
- 操作系统: Linux (Ubuntu 20.04+ / CentOS 7+)
- Java: OpenJDK 17 或更高版本
- Python: 3.9 或更高版本
- MySQL: 8.0 或更高版本
- Redis: 6.0 或更高版本 (可选)

**安装命令 (Ubuntu)：**
```bash
# 更新系统
sudo apt update && sudo apt upgrade -y

# 安装 Java 17
sudo apt install openjdk-17-jdk -y
java -version

# 安装 Python 3.9+
sudo apt install python3 python3-pip -y
python3 --version

# 安装 MySQL 8.0
sudo apt install mysql-server -y
sudo systemctl start mysql
sudo systemctl enable mysql

# 安装 Redis (可选)
sudo apt install redis-server -y
sudo systemctl start redis
sudo systemctl enable redis
```

### 2. 数据库初始化

```bash
# 登录 MySQL
mysql -u root -p

# 创建数据库
CREATE DATABASE `cidi-db` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# 创建用户并授权
CREATE USER 'cidi_user'@'%' IDENTIFIED BY 'your_strong_password';
GRANT ALL PRIVILEGES ON `cidi-db`.* TO 'cidi_user'@'%';
FLUSH PRIVILEGES;

# 导入数据库结构
USE `cidi-db`;
SOURCE /path/to/devs/script/sql/cidi_db.sql;
```

### 3. 配置环境变量

创建环境变量配置文件：

```bash
# 创建配置目录
sudo mkdir -p /opt/cidi
sudo vim /opt/cidi/env.sh
```

添加以下内容：

```bash
#!/bin/bash

# Spring 配置
export SPRING_PROFILES_ACTIVE=prod

# 数据库配置
export SPRING_DATASOURCE_URL='jdbc:mysql://localhost:3306/cidi-db?useSSL=false&serverTimezone=UTC'
export SPRING_DATASOURCE_USERNAME='cidi_user'
export SPRING_DATASOURCE_PASSWORD='your_strong_password'

# 微信配置
export WX_APPID_PROD='your_wechat_appid'
export WX_SECRET_PROD='your_wechat_secret'

# JWT 配置
export JWT_SECRET_PROD='your_jwt_secret_at_least_32_characters_long'
export JWT_EXPIRATION=604800000

# Redis 配置 (可选)
# export SPRING_REDIS_HOST=localhost
# export SPRING_REDIS_PORT=6379
```

设置权限：

```bash
sudo chmod 600 /opt/cidi/env.sh
sudo chown root:root /opt/cidi/env.sh
```

### 4. 运行部署前检查

在本地或服务器上运行自动化检查脚本：

```bash
cd /path/to/Daiso
bash devs/script/pre_deploy_check.sh
```

检查项包括：
- ✓ 代码质量（TODO、调试输出）
- ✓ 安全检查（硬编码密码、SQL 注入）
- ✓ 配置检查（生产配置、JPA 设置）
- ✓ 依赖检查（版本、SNAPSHOT）
- ✓ 构建测试（Maven 构建）
- ✓ Git 状态（未提交更改、分支）
- ✓ 文档完整性
- ✓ 数据库脚本
- ✓ 性能优化（分页、缓存）

**必须确保所有严重问题（红色 ✗）都已修复才能继续部署！**

---

## 自动化部署

### 方式一：使用一键部署脚本（推荐）

```bash
# 1. 加载环境变量
source /opt/cidi/env.sh

# 2. 运行部署脚本
cd /path/to/Daiso
bash devs/script/deploy.sh prod
```

脚本会自动执行：
1. ✓ 检查环境变量
2. ✓ 运行部署前检查
3. ✓ 备份数据库（生产环境）
4. ✓ 构建 Java 后端
5. ✓ 停止旧服务
6. ✓ 启动新服务
7. ✓ 健康检查
8. ✓ 输出部署总结

### 方式二：使用 systemd 服务（推荐生产环境）

创建 systemd 服务文件：

```bash
sudo vim /etc/systemd/system/cidi.service
```

添加以下内容：

```ini
[Unit]
Description=CIDI Tourism Service
After=network.target mysql.service

[Service]
Type=simple
User=cidi
Group=cidi
WorkingDirectory=/opt/cidi

# 加载环境变量
EnvironmentFile=/opt/cidi/env.sh

# 启动命令
ExecStart=/usr/bin/java -jar /opt/cidi/cidi-0.0.1-SNAPSHOT.jar

# 日志
StandardOutput=append:/opt/cidi/logs/app.log
StandardError=append:/opt/cidi/logs/error.log

# 重启策略
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

创建服务用户和目录：

```bash
# 创建用户
sudo useradd -r -s /bin/false cidi

# 创建目录
sudo mkdir -p /opt/cidi/logs
sudo chown -R cidi:cidi /opt/cidi
```

部署 JAR 文件：

```bash
# 本地构建
cd /path/to/Daiso/devs/backend/cidi-server
./mvnw clean package -DskipTests

# 上传到服务器
scp target/cidi-0.0.1-SNAPSHOT.jar user@server:/opt/cidi/
```

启动服务：

```bash
# 重载 systemd
sudo systemctl daemon-reload

# 启动服务
sudo systemctl start cidi

# 查看状态
sudo systemctl status cidi

# 设置开机自启
sudo systemctl enable cidi

# 查看日志
sudo journalctl -u cidi -f
```

---

## 手动部署

如果自动化脚本不可用，可以手动执行以下步骤：

### 1. 构建项目

```bash
cd /path/to/Daiso/devs/backend/cidi-server
./mvnw clean package -DskipTests
```

### 2. 上传到服务器

```bash
scp target/cidi-0.0.1-SNAPSHOT.jar user@server:/opt/cidi/
```

### 3. 停止旧服务

```bash
ssh user@server
ps aux | grep cidi.*jar
kill <PID>
```

### 4. 启动新服务

```bash
cd /opt/cidi
source env.sh

nohup java -jar cidi-0.0.1-SNAPSHOT.jar > logs/app.log 2>&1 &
echo $! > logs/app.pid
```

### 5. 验证服务

```bash
# 等待启动
sleep 10

# 测试接口
curl http://localhost:8080/api/posts
```

---

## 部署验证

### 1. 使用健康检查脚本

```bash
bash devs/script/test_api_health.sh http://your-server-ip:8080
```

检查项包括：
- ✓ 服务器连通性
- ✓ 核心接口（动态、景区、图片）
- ✓ API 文档访问
- ✓ 错误处理
- ✓ 接口响应时间

### 2. 手动验证核心功能

**基础接口：**
```bash
# 获取动态列表
curl http://your-server:8080/api/posts

# 获取景区列表
curl http://your-server:8080/api/locations

# 获取景区图片
curl http://your-server:8080/api/location-images
```

**API 文档：**
```bash
# 访问 Swagger UI
http://your-server:8080/doc.html
```

**日志检查：**
```bash
# 查看应用日志
tail -f /opt/cidi/logs/app.log

# 检查错误日志
grep -i error /opt/cidi/logs/app.log
```

### 3. 前端配置

修改小程序 API 地址：

```javascript
// devs/frontend/cidi-wx/app.js
globalData: {
  apiBaseUrl: 'http://your-server-ip:8080',  // 或 https://your-domain.com
  // ...
}
```

**注意：**
- 生产环境必须使用 HTTPS
- 需在微信公众平台配置合法域名
- 域名必须备案

---

## 常见问题

### 1. 服务启动失败

**问题：** 服务启动后立即退出

**排查步骤：**
```bash
# 查看日志
tail -100 /opt/cidi/logs/app.log

# 检查端口占用
netstat -tuln | grep 8080

# 检查 Java 版本
java -version  # 必须是 17+

# 检查环境变量
echo $SPRING_PROFILES_ACTIVE
echo $SPRING_DATASOURCE_URL
```

**常见原因：**
- 数据库连接失败（检查 URL、用户名、密码）
- 端口被占用（修改端口或停止占用进程）
- 环境变量未加载（source env.sh）
- Java 版本不兼容（升级到 Java 17+）

### 2. 数据库连接失败

**错误信息：** `Communications link failure`

**解决方案：**
```bash
# 检查 MySQL 是否运行
sudo systemctl status mysql

# 检查防火墙
sudo ufw status
sudo ufw allow 3306

# 检查 MySQL 绑定地址
sudo vim /etc/mysql/mysql.conf.d/mysqld.cnf
# 确保 bind-address = 0.0.0.0

# 重启 MySQL
sudo systemctl restart mysql

# 测试连接
mysql -h localhost -u cidi_user -p
```

### 3. 接口返回 404

**问题：** 所有接口都返回 404

**排查步骤：**
```bash
# 检查服务是否启动
ps aux | grep cidi.*jar

# 检查端口监听
netstat -tuln | grep 8080

# 检查日志
grep -i "started" /opt/cidi/logs/app.log
```

**常见原因：**
- 服务未完全启动（等待 30 秒后重试）
- 端口配置错误（检查 server.port）
- 路径前缀错误（确保使用 /api/*）

### 4. 前端无法连接后端

**问题：** 小程序请求失败

**排查步骤：**
```bash
# 检查服务器防火墙
sudo ufw status
sudo ufw allow 8080

# 检查云服务器安全组
# 在云服务商控制台开放 8080 端口

# 测试外网访问
curl http://your-public-ip:8080/api/posts
```

**常见原因：**
- 防火墙未开放端口
- 云服务器安全组未配置
- 小程序域名未配置（生产环境必须 HTTPS）
- apiBaseUrl 仍为 localhost

### 5. 内存不足

**错误信息：** `OutOfMemoryError`

**解决方案：**
```bash
# 限制 JVM 内存
java -Xmx1024m -Xms512m -jar cidi-0.0.1-SNAPSHOT.jar

# 或在 systemd 服务中添加
ExecStart=/usr/bin/java -Xmx1024m -Xms512m -jar /opt/cidi/cidi-0.0.1-SNAPSHOT.jar
```

---

## 回滚方案

### 1. 快速回滚

如果新版本有问题，立即回滚到旧版本：

```bash
# 停止当前服务
sudo systemctl stop cidi

# 恢复旧版本 JAR
cd /opt/cidi
mv cidi-0.0.1-SNAPSHOT.jar cidi-new.jar
mv cidi-old.jar cidi-0.0.1-SNAPSHOT.jar

# 启动服务
sudo systemctl start cidi

# 验证
curl http://localhost:8080/api/posts
```

### 2. 数据库回滚

如果数据库有变更，恢复备份：

```bash
# 查看备份文件
ls -lh /path/to/Daiso/backups/

# 恢复数据库
mysql -u cidi_user -p cidi-db < backups/cidi_db_20260506_120000.sql
```

### 3. 版本管理建议

```bash
# 部署时保留旧版本
cd /opt/cidi
cp cidi-0.0.1-SNAPSHOT.jar cidi-0.0.1-SNAPSHOT.jar.$(date +%Y%m%d_%H%M%S)

# 定期清理旧版本（保留最近 3 个）
ls -t cidi-*.jar.* | tail -n +4 | xargs rm -f
```

---

## 监控与维护

### 1. 日志管理

```bash
# 实时查看日志
tail -f /opt/cidi/logs/app.log

# 查看错误日志
grep -i error /opt/cidi/logs/app.log

# 日志轮转配置
sudo vim /etc/logrotate.d/cidi
```

添加以下内容：

```
/opt/cidi/logs/*.log {
    daily
    rotate 7
    compress
    delaycompress
    missingok
    notifempty
    create 0644 cidi cidi
}
```

### 2. 性能监控

```bash
# 查看 Java 进程资源使用
top -p $(cat /opt/cidi/logs/app.pid)

# 查看内存使用
jmap -heap $(cat /opt/cidi/logs/app.pid)

# 查看线程状态
jstack $(cat /opt/cidi/logs/app.pid)
```

### 3. 定期维护

```bash
# 每周检查磁盘空间
df -h

# 每月清理日志
find /opt/cidi/logs -name "*.log.*" -mtime +30 -delete

# 每月备份数据库
mysqldump -u cidi_user -p cidi-db > backup_$(date +%Y%m%d).sql
```

---

## 安全加固

### 1. 防火墙配置

```bash
# 只开放必要端口
sudo ufw default deny incoming
sudo ufw default allow outgoing
sudo ufw allow ssh
sudo ufw allow 8080/tcp
sudo ufw enable
```

### 2. SSL/TLS 配置

生产环境必须使用 HTTPS：

```bash
# 安装 Nginx
sudo apt install nginx -y

# 配置反向代理
sudo vim /etc/nginx/sites-available/cidi
```

添加以下内容：

```nginx
server {
    listen 80;
    server_name your-domain.com;
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name your-domain.com;

    ssl_certificate /path/to/cert.pem;
    ssl_certificate_key /path/to/key.pem;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

启用配置：

```bash
sudo ln -s /etc/nginx/sites-available/cidi /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl restart nginx
```

### 3. 定期更新

```bash
# 更新系统
sudo apt update && sudo apt upgrade -y

# 更新依赖
cd /path/to/Daiso/devs/backend/cidi-server
./mvnw versions:display-dependency-updates
```

---

## 总结

本部署指南提供了完整的生产环境部署流程，包括：

✓ 自动化部署脚本（推荐）
✓ systemd 服务管理（生产环境）
✓ 完整的验证流程
✓ 常见问题排查
✓ 回滚方案
✓ 监控与维护

**部署前必读：**
1. 完成 [PRODUCTION_CHECKLIST.md](./PRODUCTION_CHECKLIST.md) 所有检查项
2. 运行 `pre_deploy_check.sh` 确保无严重问题
3. 在测试环境完整验证
4. 准备回滚方案

**紧急联系：**
- 技术负责人：[联系方式]
- 运维负责人：[联系方式]
