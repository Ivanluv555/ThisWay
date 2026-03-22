# CIDI 前后端通信与最小联调说明

本说明基于当前代码现状整理，目标是让前端小程序与 Java 后端能够稳定通信，并给出后端打包上线与最小联调流程。

---

## 1. 当前代码现状（已核对）

### 1.1 前端（微信小程序）

- 请求封装文件：`devs/frontend/cidi-wx/api/http.js`
- 基址来源：`getApp().globalData.apiBaseUrl`
- 默认基址：`http://localhost:8080`
- 请求路径示例：`/api/posts`、`/api/auth/wechat/login`

### 1.2 后端（Spring Boot）

- 主配置文件：`devs/backend/cidi-server/src/main/resources/application.properties`
- 端口：`server.port=8080`
- 监听地址：`server.address=0.0.0.0`
- Profile 切换：`SPRING_PROFILES_ACTIVE`（默认 `dev`）
- 关键环境变量已支持：数据库、微信配置、JWT

结论：当前前后端路径前缀是一致的（都走 `/api/*`），通信是否成功主要取决于“前端基址是否指向可访问的后端地址”与“后端环境变量是否正确”。

---

## 2. 推荐通信方式（环境变量优先）

### 2.1 后端必须通过环境变量注入

至少设置如下变量（生产环境）：

- `SPRING_PROFILES_ACTIVE=prod`
- `SPRING_DATASOURCE_URL=jdbc:mysql://<db-host>:3306/cidi-db?useSSL=false&serverTimezone=UTC`
- `SPRING_DATASOURCE_USERNAME=<db-user>`
- `SPRING_DATASOURCE_PASSWORD=<db-password>`
- `WX_APPID_PROD=<wechat-appid>`
- `WX_SECRET_PROD=<wechat-secret>`
- `JWT_SECRET_PROD=<strong-jwt-secret>`

可选：

- `JWT_EXPIRATION=604800000`

不要在文档、代码仓库或配置文件中保留明文密钥。

### 2.2 前端基址必须改为服务器地址

修改文件 `devs/frontend/cidi-wx/app.js` 中的 `apiBaseUrl`：

- 本地联调（开发者工具 + 本机后端）：`http://127.0.0.1:8080`
- 局域网联调：`http://<局域网IP>:8080`
- 服务器联调：`http://<服务器公网IP>:8080` 或 `https://<域名>`

说明：

- 如果用真机调试，`localhost` 指向手机自身，不是你的电脑。
- 小程序正式版通常要求 HTTPS 与合法域名，请按微信平台要求配置服务器域名。

---

## 3. 后端打包并发送到服务器

以下步骤在项目根目录执行，假设服务器已安装 Java 17+。

### 3.1 本地打包

```bash
cd devs/backend/cidi-server
./mvnw -DskipTests clean package
```

产物位置（默认）：

- `devs/backend/cidi-server/target/cidi-0.0.1-SNAPSHOT.jar`

### 3.2 发送到服务器

```bash
scp devs/backend/cidi-server/target/cidi-0.0.1-SNAPSHOT.jar <user>@<server-ip>:/opt/cidi/
```

### 3.3 服务器启动（环境变量方式）

```bash
ssh <user>@<server-ip>
cd /opt/cidi

export SPRING_PROFILES_ACTIVE=prod
export SPRING_DATASOURCE_URL='jdbc:mysql://<db-host>:3306/cidi-db?useSSL=false&serverTimezone=UTC'
export SPRING_DATASOURCE_USERNAME='<db-user>'
export SPRING_DATASOURCE_PASSWORD='<db-password>'
export WX_APPID_PROD='<wechat-appid>'
export WX_SECRET_PROD='<wechat-secret>'
export JWT_SECRET_PROD='<strong-jwt-secret>'

nohup java -jar cidi-0.0.1-SNAPSHOT.jar > app.log 2>&1 &
```

快速验证：

```bash
curl http://127.0.0.1:8080/api/posts
```

---

## 4. 前后端最小联调清单

只做最小闭环，建议按下面顺序验证。

### 4.1 基础可达性

1. 浏览器或 curl 访问 `GET /api/posts` 返回 200。
2. 小程序首页能拉取动态列表。

### 4.2 登录链路

1. 调用 `POST /api/auth/wechat/login` 返回 `token` 与 `user`。
2. 小程序本地缓存 token 与 userInfo。

### 4.3 核心读写

1. 地图：`GET /api/locations`、`GET /api/location-images`。
2. 搜索：`GET /api/posts`、`GET /api/locations`、`POST /api/search-history`。
3. 互动：`POST /api/actions`、`POST /api/comments`。

### 4.4 常见故障排查

1. 401/403：当前代码未完整启用统一鉴权拦截，优先排查是否请求地址错误。
2. 404：检查路径是否为 `/api/*`（不是 `/api/v1/*`）。
3. 小程序请求失败：检查 `apiBaseUrl` 是否仍是 `localhost`。
4. 登录失败：检查微信环境变量是否与当前 profile 对应。
5. 数据库报错：检查 `SPRING_DATASOURCE_*` 与网络白名单。

---

## 5. 建议的后续优化（可选）

1. 在后端补充统一鉴权过滤器（Bearer Token）并文档化。
2. 在前端新增 `env.js` 配置文件，按开发/测试/生产切换 `apiBaseUrl`。
3. 使用 systemd 或 Docker 管理后端进程，替代 `nohup`。
