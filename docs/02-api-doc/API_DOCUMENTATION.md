# CIDI 服务端 API 接口文档

**版本**: 1.1  
**日期**: 2026-03-22  
**状态**: 已按当前后端实现更新

---

## 1. 文档说明

本文档描述的是当前 Spring Boot 后端已经实现的离线 API 现状，而不是早期规划中的目标接口。

当前实现有两个明显特征：

1. 资源接口统一采用 `/api/*` 路径，而不是旧文档中的 `/api/v1/*`。
2. 绝大多数模块是标准 CRUD 控制器，直接返回实体 JSON，不做统一 `code/message/data` 成功包装。

---

## 2. 基础信息

### 2.1 Base URL

当前代码中的基础路径如下：

- 普通资源接口：`/api`
- 微信认证接口：`/api/auth/wechat`

### 2.2 数据格式

- 请求格式：`application/json`
- 响应格式：`application/json`
- 字符编码：`UTF-8`

### 2.3 认证现状

- 已实现：微信登录、微信手机号绑定
- 会返回：JWT Token
- 当前代码中未看到统一的 Spring Security / JWT 拦截配置
- 结论：认证接口可返回 Token，但通用 CRUD 接口当前文档层面不能宣称“已强制鉴权”

### 2.4 成功响应格式

当前成功响应通常直接返回实体对象、实体列表或空响应：

```json
{
  "id": 1,
  "username": "alice"
}
```

列表响应示例：

```json
[
  {
    "id": 1,
    "username": "alice"
  }
]
```

删除成功响应：

- HTTP `204 No Content`

### 2.5 错误响应格式

错误统一由全局异常处理器返回：

```json
{
  "timestamp": "2026-03-22T16:30:00",
  "status": 404,
  "code": "RESOURCE_NOT_FOUND",
  "message": "资源不存在: id=1",
  "path": "/api/users/1"
}
```

---

## 3. 已实现接口总览

当前后端已实现以下控制器：

- `/api/auth/wechat`
- `/api/users`
- `/api/user-settings`
- `/api/user-follows`
- `/api/locations`
- `/api/location-images`
- `/api/spot-traffic-predictions`
- `/api/categories`
- `/api/posts`
- `/api/post-images`
- `/api/comments`
- `/api/actions`
- `/api/hot-searches`
- `/api/search-history`
- `/api/feedbacks`
- `/api/feedback-images`
- `/api/admins`

以上资源接口都支持同一套基础 CRUD：

- `GET /resource`
- `GET /resource/{id}`
- `POST /resource`
- `PUT /resource/{id}`
- `DELETE /resource/{id}`

当前未实现旧文档中的这些规划式接口：

- `/users/me`
- `/users/me/settings`
- `/users/{userId}/follow`
- `/users/{userId}/following`
- `/users/{userId}/followers`
- `/locations/{id}/traffic`
- `/posts/{postId}/comments`
- `/posts/{postId}/actions`
- `/search/hot`
- `/search`
- `/search/history`
- `/auth/admin/login`
- `/admin/feedbacks`
- `/admin/feedbacks/{id}/status`

---

## 4. 认证接口

### 4.1 微信用户登录

- **POST** `/api/auth/wechat/login`
- **描述**: 小程序端登录/注册。后端通过微信 `code` 换取 `openid`，若用户不存在则自动创建。

#### Request Body

```json
{
  "code": "061xxxxx",
  "nickname": "微信昵称",
  "avatarUrl": "https://example.com/avatar.png"
}
```

#### Response Body

```json
{
  "token": "jwt-token",
  "expiresAt": "2026-03-22T18:00:00",
  "wechatEnv": "prod",
  "user": {
    "id": 1,
    "openid": "wx_openid_xxx",
    "username": "微信昵称",
    "avatarUrl": "https://example.com/avatar.png",
    "bio": null,
    "phone": null,
    "status": 1,
    "createTime": "2026-03-22T16:00:00",
    "lastLoginTime": "2026-03-22T16:00:00"
  }
}
```

#### 说明

- `nickname` 会写入用户字段 `username`
- `avatarUrl` 会写入用户字段 `avatarUrl`

### 4.2 微信手机号绑定

- **POST** `/api/auth/wechat/phone/bind`
- **描述**: 使用微信手机号 code 绑定手机号到现有用户。

#### Request Body

```json
{
  "userId": 1,
  "code": "phone-code"
}
```

#### Response Body

```json
{
  "userId": 1,
  "phone": "13800000000",
  "newlyBound": true
}
```

### 4.3 管理员登录

当前代码中**未实现** `/auth/admin/login`。

---

## 5. 通用 CRUD 约定

除认证接口外，所有资源控制器基本遵循以下行为：

### 5.1 获取列表

- **GET** `/api/{resource}`
- 返回该资源的全部记录列表
- 当前未实现分页、筛选、排序参数

### 5.2 获取单条记录

- **GET** `/api/{resource}/{id}`
- 若不存在，返回 `404 RESOURCE_NOT_FOUND`

### 5.3 新增记录

- **POST** `/api/{resource}`
- 请求体直接为实体 JSON
- 返回保存后的实体

### 5.4 更新记录

- **PUT** `/api/{resource}/{id}`
- 路径参数中的 `id` 会覆盖实体主键
- 若记录不存在，返回 `404 RESOURCE_NOT_FOUND`

### 5.5 删除记录

- **DELETE** `/api/{resource}/{id}`
- 成功返回 `204 No Content`

---

## 6. 用户域

### 6.1 用户资源

- **资源路径**: `/api/users`

#### 字段结构

```json
{
  "id": 1,
  "openid": "wx_openid_xxx",
  "username": "alice",
  "avatarUrl": "https://example.com/avatar.png",
  "bio": "city walker",
  "phone": "13800000001",
  "status": 1,
  "createTime": "2026-03-22T10:00:00",
  "lastLoginTime": "2026-03-22T12:00:00"
}
```

#### 已实现接口

- `GET /api/users`
- `GET /api/users/{id}`
- `POST /api/users`
- `PUT /api/users/{id}`
- `DELETE /api/users/{id}`

#### 与旧文档差异

- 当前字段为 `username`，不是 `nickname`
- 当前未实现 `/users/me`

### 6.2 用户设置资源

- **资源路径**: `/api/user-settings`

#### 字段结构

```json
{
  "userId": 1,
  "isPrivateAccount": 0,
  "pushNotifications": 1,
  "themeColor": "system",
  "updateTime": "2026-03-22T09:00:00"
}
```

#### 已实现接口

- `GET /api/user-settings`
- `GET /api/user-settings/{id}`
- `POST /api/user-settings`
- `PUT /api/user-settings/{id}`
- `DELETE /api/user-settings/{id}`

#### 说明

- 这里的 `{id}` 实际上是 `userId`
- 当前字段不是旧文档中的 `notification_enabled` / `privacy_level`

### 6.3 用户关注关系资源

- **资源路径**: `/api/user-follows`

#### 字段结构

```json
{
  "id": 1,
  "followerId": 1,
  "followedId": 2,
  "createTime": "2026-03-22T09:00:00"
}
```

#### 已实现接口

- `GET /api/user-follows`
- `GET /api/user-follows/{id}`
- `POST /api/user-follows`
- `PUT /api/user-follows/{id}`
- `DELETE /api/user-follows/{id}`

#### 与旧文档差异

- 当前未实现“按用户关注/取关”的语义化接口

---

## 7. 位置服务域

### 7.1 地点资源

- **资源路径**: `/api/locations`

#### 字段结构

```json
{
  "id": 1,
  "title": "West Lake",
  "address": "Hangzhou, Zhejiang",
  "description": "Classic city lake",
  "heatScore": 92,
  "category": "scenery",
  "rating": 4.8,
  "latitude": 30.243060,
  "longitude": 120.150164
}
```

#### 已实现接口

- `GET /api/locations`
- `GET /api/locations/{id}`
- `POST /api/locations`
- `PUT /api/locations/{id}`
- `DELETE /api/locations/{id}`

#### 与旧文档差异

- 当前未实现 keyword、radius、distance 等查询参数
- 当前地点名称字段为 `title`，不是 `name`

### 7.2 地点图片资源

- **资源路径**: `/api/location-images`

#### 字段结构

```json
{
  "id": 1,
  "locationId": 1,
  "imageUrl": "https://example.com/location.jpg",
  "isCover": 1,
  "sortOrder": 1
}
```

#### 已实现接口

- `GET /api/location-images`
- `GET /api/location-images/{id}`
- `POST /api/location-images`
- `PUT /api/location-images/{id}`
- `DELETE /api/location-images/{id}`

### 7.3 景点流量预测资源

- **资源路径**: `/api/spot-traffic-predictions`

#### 字段结构

```json
{
  "id": 1,
  "spotId": 1,
  "predictTime": "2026-03-23T10:00:00",
  "visitorCount": 3600,
  "congestionLevel": 2,
  "trend": "up",
  "trafficLevel": 3,
  "crowdDensity": 72.50,
  "createTime": "2026-03-22T08:00:00"
}
```

#### 已实现接口

- `GET /api/spot-traffic-predictions`
- `GET /api/spot-traffic-predictions/{id}`
- `POST /api/spot-traffic-predictions`
- `PUT /api/spot-traffic-predictions/{id}`
- `DELETE /api/spot-traffic-predictions/{id}`

#### 与旧文档差异

- 当前未实现 `/locations/{id}/traffic`
- 当前需要由客户端按 `spotId` 自行筛选

---

## 8. 内容域

### 8.1 分类资源

- **资源路径**: `/api/categories`

#### 字段结构

```json
{
  "id": 1,
  "name": "food",
  "iconUrl": "https://example.com/icon.png",
  "sortOrder": 1,
  "isActive": 1
}
```

#### 已实现接口

- `GET /api/categories`
- `GET /api/categories/{id}`
- `POST /api/categories`
- `PUT /api/categories/{id}`
- `DELETE /api/categories/{id}`

### 8.2 动态资源

- **资源路径**: `/api/posts`

#### 字段结构

```json
{
  "id": 1,
  "userId": 1,
  "categoryId": 1,
  "locationId": 1,
  "title": "Morning at West Lake",
  "content": "Great weather today",
  "viewCount": 300,
  "likeCount": 23,
  "createTime": "2026-03-22T10:00:00"
}
```

#### 已实现接口

- `GET /api/posts`
- `GET /api/posts/{id}`
- `POST /api/posts`
- `PUT /api/posts/{id}`
- `DELETE /api/posts/{id}`

#### 与旧文档差异

- 当前发布帖子时，图片不在 `/api/posts` 一次性嵌套提交
- 图片需单独调用 `/api/post-images`
- 当前未实现分页、按分类/地点/用户筛选、hot/latest 排序

### 8.3 动态图片资源

- **资源路径**: `/api/post-images`

#### 字段结构

```json
{
  "id": 1,
  "postId": 1,
  "imageUrl": "https://example.com/post.jpg",
  "width": 800,
  "height": 600,
  "sortOrder": 1
}
```

#### 已实现接口

- `GET /api/post-images`
- `GET /api/post-images/{id}`
- `POST /api/post-images`
- `PUT /api/post-images/{id}`
- `DELETE /api/post-images/{id}`

---

## 9. 交互域

### 9.1 评论资源

- **资源路径**: `/api/comments`

#### 字段结构

```json
{
  "id": 1,
  "postId": 1,
  "userId": 2,
  "parentId": null,
  "content": "写得很好",
  "createTime": "2026-03-22T10:00:00"
}
```

#### 已实现接口

- `GET /api/comments`
- `GET /api/comments/{id}`
- `POST /api/comments`
- `PUT /api/comments/{id}`
- `DELETE /api/comments/{id}`

#### 与旧文档差异

- 当前未实现 `/posts/{postId}/comments`
- 当前需要客户端按 `postId` 自行筛选

### 9.2 点赞/收藏资源

- **资源路径**: `/api/actions`

#### 字段结构

```json
{
  "id": 1,
  "userId": 1,
  "postId": 3,
  "type": "like",
  "createTime": "2026-03-22T10:00:00"
}
```

#### 说明

- `type` 当前为字符串：`like` 或 `collect`
- 不再使用旧模型中的 `action_type`、`target_id`、`target_type`

#### 已实现接口

- `GET /api/actions`
- `GET /api/actions/{id}`
- `POST /api/actions`
- `PUT /api/actions/{id}`
- `DELETE /api/actions/{id}`

#### 与旧文档差异

- 当前未实现 `/posts/{postId}/actions`

---

## 10. 搜索与发现

### 10.1 热搜资源

- **资源路径**: `/api/hot-searches`

#### 字段结构

```json
{
  "id": 1,
  "keyword": "west lake",
  "hotValue": 980,
  "isActive": 1
}
```

#### 已实现接口

- `GET /api/hot-searches`
- `GET /api/hot-searches/{id}`
- `POST /api/hot-searches`
- `PUT /api/hot-searches/{id}`
- `DELETE /api/hot-searches/{id}`

### 10.2 搜索历史资源

- **资源路径**: `/api/search-history`

#### 字段结构

```json
{
  "id": 1,
  "userId": 1,
  "keyword": "west lake route",
  "isDeleted": 0,
  "createTime": "2026-03-22T10:00:00"
}
```

#### 已实现接口

- `GET /api/search-history`
- `GET /api/search-history/{id}`
- `POST /api/search-history`
- `PUT /api/search-history/{id}`
- `DELETE /api/search-history/{id}`

#### 与旧文档差异

- 当前未实现综合搜索接口 `/search`
- 当前未实现 `/search/history` 和 `/search/hot`

---

## 11. 反馈与管理

### 11.1 用户反馈资源

- **资源路径**: `/api/feedbacks`

#### 字段结构

```json
{
  "id": 1,
  "userId": 1,
  "content": "Map marker overlaps",
  "contactInfo": "alice@example.com",
  "status": 0,
  "createTime": "2026-03-22T10:00:00"
}
```

#### 已实现接口

- `GET /api/feedbacks`
- `GET /api/feedbacks/{id}`
- `POST /api/feedbacks`
- `PUT /api/feedbacks/{id}`
- `DELETE /api/feedbacks/{id}`

### 11.2 反馈图片资源

- **资源路径**: `/api/feedback-images`

#### 字段结构

```json
{
  "id": 1,
  "feedbackId": 1,
  "imageUrl": "https://example.com/f1.png"
}
```

#### 已实现接口

- `GET /api/feedback-images`
- `GET /api/feedback-images/{id}`
- `POST /api/feedback-images`
- `PUT /api/feedback-images/{id}`
- `DELETE /api/feedback-images/{id}`

### 11.3 管理员资源

- **资源路径**: `/api/admins`

#### 字段结构

```json
{
  "id": 1,
  "job": "platform_ops",
  "username": "admin01",
  "password": "hash_xxx",
  "role": "super_admin",
  "createTime": "2026-03-22T10:00:00"
}
```

#### 已实现接口

- `GET /api/admins`
- `GET /api/admins/{id}`
- `POST /api/admins`
- `PUT /api/admins/{id}`
- `DELETE /api/admins/{id}`

#### 与旧文档差异

- 当前仅实现管理员资源 CRUD
- 当前未实现管理员登录接口与后台专用反馈处理接口

---

## 12. 前端联调建议

基于当前实现，前端联调应遵循以下原则：

1. 不要再按旧文档调用 `/api/v1`。
2. 不要假设成功响应有统一 `code/message/data` 包装。
3. 评论、收藏、热搜、搜索历史、流量预测都需要前端自己按字段过滤聚合。
4. 点赞/收藏字段使用：`postId + type`。
5. 用户字段使用：`username`，不是 `nickname`。
6. 地点字段使用：`title`，不是 `name`。
7. 流量预测字段使用：`spotId`、`predictTime`，不是 `locationId`、`predictionTime`。

---

## 13. 后续建议

若后续要把“离线文档”继续往“正式接口规范”推进，建议新增以下内容：

1. 补充 Spring Security/JWT 鉴权中间件后，再更新鉴权章节。
2. 为列表接口增加分页、筛选、排序参数。
3. 为评论、动作、搜索、地点流量预测补充资源化聚合接口，减少前端多次请求。
4. 为管理员补充登录与反馈处理专用接口。


