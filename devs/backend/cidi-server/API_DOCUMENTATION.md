# CIDI 服务端 API 接口文档

**版本**: 1.0  
**日期**: 2026-03-11  
**状态**: 草稿 (Draft)

---

## 1. 概览 (Overview)

本配套文档基于《数据库设计说明书》编写，定义了 CIDI 服务端的核心 API 接口。

### 1.1 基础信息
*   **Base URL**: `/api/v1` (建议)
*   **数据格式**: JSON
*   **字符编码**: UTF-8

### 1.2 认证鉴权
*   **认证方式**: Bearer Token (JWT)
*   **Header**: `Authorization: Bearer <token>`
*   **Token 获取**:
    *   **管理员**: `/auth/admin/login`
    *   **C端用户**: `/auth/wechat/login` (基于 `openid`)

### 1.3 通用响应结构
```json
{
  "code": 200,          // 状态码 (200成功, 其他均为异常)
  "message": "success", // 提示信息
  "data": { ... }       // 业务数据
}
```

---

## 2. 认证与授权 (Auth Module)

### 2.1 管理员登录
*   **POST** `/auth/admin/login`
*   **描述**: 后台管理员登录，获取 JWT Token。
*   **Body**:
    ```json
    {
      "username": "admin",
      "password": "password123"
    }
    ```
*   **Response**: `token`, `admin_info`

### 2.2 微信用户登录
*   **POST** `/auth/wechat/login`
*   **描述**: 小程序端用户登录/注册。根据 `wx.login` 获取的 code 换取 openid，自动注册或登录。
*   **Body**:
    ```json
    {
      "code": "061......" // 微信临时登录凭证
    }
    ```
*   **Response**: `token`, `user_info`

---

## 3. 用户域 (User Domain)

对应数据库表: `users`, `user_settings`, `user_follows`

### 3.1 获取当前用户信息
*   **GET** `/users/me`
*   **描述**: 获取当前登录用户的详细信息及设置。

### 3.2 更新用户信息
*   **PUT** `/users/me`
*   **Body**:
    ```json
    {
      "nickname": "新昵称",
      "avatar_url": "http://..."
    }
    ```

### 3.3 用户设置
*   **GET** `/users/me/settings`
*   **PUT** `/users/me/settings`
*   **Body**:
    ```json
    {
      "notification_enabled": 1,
      "privacy_level": 0
    }
    ```

### 3.4 关注/取消关注
*   **POST** `/users/{userId}/follow`
*   **DELETE** `/users/{userId}/follow`

### 3.5 关注列表/粉丝列表
*   **GET** `/users/{userId}/following`
*   **GET** `/users/{userId}/followers`

---

## 4. 位置服务域 (LBS Domain)

对应数据库表: `locations`, `location_images`, `spot_traffic_predictions`

### 4.1 获取地点列表 (POI)
*   **GET** `/locations`
*   **Query Params**:
    *   `category`: 分类筛选
    *   `keyword`: 名称搜索
    *   `latitude`: 用户当前纬度 (用于计算距离)
    *   `longitude`: 用户当前经度
    *   `radius`: 搜索半径 (米)
*   **Response**: 地点列表 (包含距离信息)

### 4.2 获取地点详情
*   **GET** `/locations/{id}`
*   **Response**: 地点基础信息、图集 (`location_images`)、当前综合评分。

### 4.3 获取地点流量预测
*   **GET** `/locations/{id}/traffic`
*   **Query Params**:
    *   `date`: 预测日期 (YYYY-MM-DD)
*   **Response**: 该地点即时的拥堵等级或未来几个时段的预测数据 (`spot_traffic_predictions`)。

---

## 5. 内容域 (Content Domain)

对应数据库表: `posts`, `post_images`, `categories`

### 5.1 获取分类列表
*   **GET** `/categories`

### 5.2 发布动态
*   **POST** `/posts`
*   **Body**:
    ```json
    {
      "category_id": 1,
      "location_id": 100, // 可选
      "title": "...",
      "content": "...",
      "images": [
        { "image_url": "...", "width": 800, "height": 600 }
      ]
    }
    ```

### 5.3 动态列表 (Feed流)
*   **GET** `/posts`
*   **Query Params**:
    *   `category_id`: 按分类筛选
    *   `location_id`: 查看某地点的动态
    *   `user_id`: 查看某人的动态
    *   `sort`: `latest` (最新) | `hot` (最热)
    *   `page`: 页码
    *   `size`: 每页数量

### 5.4 动态详情
*   **GET** `/posts/{id}`

### 5.5 删除动态
*   **DELETE** `/posts/{id}`
*   **权限**: 仅限作者或管理员。

---

## 6. 交互域 (Interaction Domain)

对应数据库表: `comments`, `actions`

### 6.1 发表评论
*   **POST** `/posts/{postId}/comments`
*   **Body**:
    ```json
    {
      "content": "写的真好!",
      "parent_id": null // 若回复评论则填评论ID
    }
    ```

### 6.2 获取评论列表
*   **GET** `/posts/{postId}/comments`

### 6.3 点赞/收藏
*   **POST** `/posts/{postId}/actions`
*   **Body**:
    ```json
    {
      "action_type": 1 // 1:点赞, 2:收藏
    }
    ```

### 6.4 取消点赞/收藏
*   **DELETE** `/posts/{postId}/actions`
*   **Query Params**:
    *   `action_type`: 1 | 2

---

## 7. 搜索与发现 (Search Domain)

对应数据库表: `hot_searches`, `search_history`

### 7.1 热搜榜单
*   **GET** `/search/hot`

### 7.2 综合搜索
*   **GET** `/search`
*   **Query Params**:
    *   `q`: 关键词
    *   `type`: `post` | `user` | `location`
*   **描述**: 搜索同时触发异步记录 `search_history` 并更新 `hot_searches`。

### 7.3 用户搜索历史
*   **GET** `/search/history`

### 7.4 清空搜索历史
*   **DELETE** `/search/history`

---

## 8. 运营与反馈 (Admin & Feedback)
*(仅限管理员权限)*

### 8.1 提交用户反馈
*   **POST** `/feedbacks` (C端用户可用)

### 8.2 反馈列表 (管理端)
*   **GET** `/admin/feedbacks`

### 8.3 处理反馈
*   **PUT** `/admin/feedbacks/{id}/status`


