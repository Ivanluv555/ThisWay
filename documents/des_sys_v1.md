<h1 style='text-align: center;'>系统架构设计文档



# 1 文档概述



## 1.1 文档版本

> 版本命名格式：v1.1	前一个数字表示一个大版本，后一个数字表示微调的小版本。一般从0开始。

v1



## 1.2 文档目的



本文档旨在详细描述景区协调分流 APP 的系统架构设计，明确系统各组成部分的功能、交互方式及技术实现方案，为开发团队提供技术指导，确保系统开发符合需求规格说明书的要求。



## 1.3 文档范围



本文档覆盖景区协调分流 APP 的整体架构设计，包括系统总体架构、技术栈选型、功能模块设计、接口设计、数据库设计、安全架构、部署架构等内容，适用于项目开发全过程。





















# 2 系统总体架构



考虑到推进项目进度，考虑快速开发，先做出一个样品，再调整。

系统采用前后端分离的轻量级架构，整体分为三层：



## 2.1 前端层（微信小程序）



- 游客端小程序：提供用户交互界面，包括人流热力图展示、路径规划、景区信息查询等功能
- 管理端小程序：供景区管理人员使用，实现人流监控、信息发布等功能



## 2.2 后端服务层（Flask）



- API 服务：提供小程序所需的所有接口服务
- 业务逻辑处理：实现用户管理、人流数据处理、路径规划等核心业务
- 第三方服务集成：对接微信接口、数据采集接口等



## 2.3 数据存储层（MongoDB）



- 存储用户信息、景区数据、人流数据、路径数据等所有业务数据
- 利用 MongoDB 的灵活性，快速适配业务需求变化























# 3 技术栈选型



## 3.1 前端技术



- 开发框架：微信小程序原生框架
- UI 组件库：Vant Weapp
- 地图组件：微信小程序地图组件（基于腾讯地图）
- 状态管理：小程序内置数据绑定
- 网络请求：wx.request 封装



## 3.2 后端技术



- Web 框架：Flask 2.0+
- API 文档：Flask-RESTX
- 数据库驱动：PyMongo
- 身份认证：基于微信小程序登录的 Session 管理
- 缓存：Flask-Caching（基于 Redis）
- 任务队列：Celery（处理异步任务如数据统计）



## 3.3 数据存储

- 主数据库：MongoDB 5.0+
- 缓存数据库：Redis 6.0+（存储热点数据、会话信息）



## 3.4 开发与部署工具

- 版本控制：Git + gitee
- 开发工具：微信开发者工具、PyCharm
- 部署：Docker + Nginx
- 监控：Flask-MonitoringExtension



## 3.5 第三方服务

- 定位服务：微信小程序 wx.getLocation 接口
- 地图服务：微信小程序地图组件（腾讯地图）
- 数据采集：模拟数据接口（后期可对接景区实际设备）





















# 4 核心功能模块设计



## 4.1 用户模块

- 微信授权登录：基于微信小程序的快捷登录
- 用户信息管理：获取并存储用户基本信息
- 权限管理：区分游客与管理员权限



## 4.2 定位与地图模块

- 位置获取：通过微信 wx.getLocation 获取用户实时位置
- 地图展示：展示景区地图，标记用户当前位置
- 景点标记：在地图上标记景区内景点、设施位置



## 4.3 人流监控模块

- 人流数据采集：模拟或对接景区实际人流统计设备
- 人流数据处理：实时处理并存储各区域人流数据
- 热力图生成：根据人流数据生成景区热力图数据
- 人流预警：当区域人流超过阈值时触发预警





## 4.4 路径规划模块

- 路径计算：基于用户当前位置、目的地和实时人流数据计算最优路径
- 路径展示：在地图上展示规划的路径
- 路径推荐：根据人流情况推荐最优游览路线



## 4.5 景区信息模块

- 景点信息管理：存储和展示景点介绍、开放时间等信息
- 设施查询：提供厕所、休息区、餐饮等设施的查询功能
- 公告发布：景区管理员发布通知公告



## 4.6 管理模块

- 数据监控：实时查看景区各区域人流数据
- 信息管理：管理景区景点、设施等信息
- 公告发布：发布和管理景区公告





















# 5 数据库设计



## 5.1 数据库选型理由

- MongoDB 作为文档型数据库， schema 灵活，适合快速开发和需求频繁变更的场景
- 支持地理空间索引，适合存储和查询地理位置信息，对路径规划功能友好
- 与 Python 生态集成良好，PyMongo 驱动使用简单
- 水平扩展能力强，可应对数据量增长



## 5.2 主要集合 (Collections) 设计



### 5.2.1 用户集合 (users)

```json
{
  "_id": ObjectId("..."),
  "openid": "微信用户唯一标识",
  "nickname": "用户昵称",
  "avatar_url": "用户头像URL",
  "role": "user|admin", // 角色
  "created_at": ISODate("..."),
  "updated_at": ISODate("...")
}
```

### 5.2.2 景区区域集合 (areas)

```json
{
  "_id": ObjectId("..."),
  "name": "区域名称",
  "description": "区域描述",
  "location": { 
    "type": "Polygon",
    "coordinates": [ [ [lng1, lat1], [lng2, lat2], ... ] ]
  },
  "max_people": 500, // 最大容纳人数
  "created_at": ISODate("..."),
  "updated_at": ISODate("...")
}
```

### 5.2.3 景点集合 (attractions)

```json
{
  "_id": ObjectId("..."),
  "name": "景点名称",
  "description": "景点描述",
  "location": { 
    "type": "Point",
    "coordinates": [longitude, latitude]
  },
  "area_id": ObjectId("..."), // 所属区域
  "opening_hours": "开放时间",
  "images": ["url1", "url2"],
  "created_at": ISODate("..."),
  "updated_at": ISODate("...")
}
```

### 5.2.4 设施集合 (facilities)

```json
{
  "_id": ObjectId("..."),
  "name": "设施名称",
  "type": "toilet|restaurant|rest_area|...", // 设施类型
  "location": { 
    "type": "Point",
    "coordinates": [longitude, latitude]
  },
  "description": "设施描述",
  "created_at": ISODate("..."),
  "updated_at": ISODate("...")
}
```

### 5.2.5 人流数据集合 (people_flow)

```json
{
  "_id": ObjectId("..."),
  "area_id": ObjectId("..."), // 区域ID
  "count": 200, // 人数
  "density": 0.4, // 密度(0-1)
  "status": "normal|crowded|full", // 状态
  "updated_at": ISODate("...")
}
```

### 5.2.6 路径集合 (routes)

```json
{
  "_id": ObjectId("..."),
  "start_point": { 
    "type": "Point",
    "coordinates": [longitude, latitude]
  },
  "end_point": { 
    "type": "Point",
    "coordinates": [longitude, latitude]
  },
  "waypoints": [
    { "type": "Point", "coordinates": [lng, lat] }
  ],
  "path": {
    "type": "LineString",
    "coordinates": [ [lng1, lat1], [lng2, lat2], ... ]
  },
  "distance": 500, // 距离(米)
  "estimated_time": 10, // 预计时间(分钟)
  "crowd_factor": 0.3, // 拥挤系数(0-1)
  "created_at": ISODate("...")
}
```



### 5.2.7 公告集合 (announcements)



```json
{
  "_id": ObjectId("..."),
  "title": "公告标题",
  "content": "公告内容",
  "author_id": ObjectId("..."), // 发布者ID
  "is_top": true|false, // 是否置顶
  "created_at": ISODate("..."),
  "updated_at": ISODate("...")
}
```

























# 6 API 接口设计

采用 RESTful 风格设计 API，主要接口如下：



### 6.1 用户相关接口

- `GET /api/user/login` - 微信登录
- `GET /api/user/info` - 获取当前用户信息



### 6.2 定位与地图接口

- `POST /api/location/update` - 更新用户位置
- `GET /api/map/areas` - 获取景区区域列表
- `GET /api/map/attractions` - 获取景点列表
- `GET /api/map/facilities` - 获取设施列表

### 6.3 人流监控接口

- `GET /api/flow/current` - 获取当前人流数据
- `GET /api/flow/heatmap` - 获取热力图数据
- `GET /api/flow/area/{id}` - 获取指定区域人流数据

### 6.4 路径规划接口

- `POST /api/route/plan` - 规划路径

```json
// 请求参数
{
  "start": {"lng": 116.xxx, "lat": 39.xxx},
  "end": {"lng": 116.xxx, "lat": 39.xxx},
  "avoid_crowded": true
}
```

- `GET /api/route/recommend` - 获取推荐路线



### 6.5 景区信息接口

- `GET /api/attraction/{id}` - 获取景点详情
- `GET /api/announcements` - 获取公告列表

### 6.6 管理接口

- `POST /api/admin/flow/update` - 更新人流数据
- `POST /api/admin/announcement` - 发布公告
- `GET /api/admin/statistics` - 获取统计数据























# 7 核心业务流程



### 7.1 用户登录流程

1. 小程序调用 wx.login 获取 code
2. 小程序将 code 发送到后端接口
3. 后端使用 code 调用微信接口获取 openid
4. 后端创建或查询用户信息，返回自定义登录态
5. 小程序存储登录态，用于后续接口调用



### 7.2 路径规划流程

1. 小程序获取用户当前位置 (wx.getLocation)
2. 用户选择目的地 (景点或设施)
3. 小程序将起点和终点发送到后端路径规划接口
4. 后端结合实时人流数据计算最优路径
5. 后端返回路径数据给小程序
6. 小程序在地图上绘制并展示路径



### 7.3 人流数据更新流程

1. 模拟数据服务或实际设备定时推送人流数据到后端
2. 后端处理数据，更新 MongoDB 中的人流集合
3. 后端计算区域拥挤状态，如超过阈值则标记为拥挤
4. 小程序定时拉取最新人流数据，更新热力图展示























# 8 部署架构

采用轻量化部署方案，适合快速开发和演示：

### 8.1 开发环境

- 本地开发：微信开发者工具 (前端) + PyCharm (后端) + 本地 MongoDB
- 代码管理：Git 仓库

### 8.2 测试 / 演示环境

- 服务器：云服务器 (2 核 4G 配置即可)
- 部署方式：Docker 容器化部署
  - 前端：小程序上传至微信公众平台 (体验版)
  - 后端：Flask 应用容器
  - 数据库：MongoDB 容器
  - 缓存：Redis 容器
- 反向代理：Nginx 处理静态资源和 API 请求转发



















# 9 关键技术与实现难点



### 9.1 微信定位与地图集成

- 实现：使用微信小程序提供的 wx.getLocation 接口获取用户位置，结合微信地图组件展示
- 难点：景区内精确定位，解决方案是结合景区地图进行坐标校准

### 9.2 路径规划算法

- 实现：基于 Dijkstra 算法，加入人流密度作为权重因子
- 优化：缓存热门路径计算结果，提高响应速度

### 9.3 实时人流数据处理

- 实现：定时任务拉取或接收人流数据，更新数据库
- 优化：使用 Redis 缓存最新人流数据，减少数据库访问

### 9.4 热力图生成

- 实现：后端根据区域人流密度生成热力图数据，前端使用地图组件叠加展示
- 优化：根据缩放级别动态调整热力图精度



















# 10 开发计划与迭代路线



### 10.1 第一阶段：基础功能 (2 周)

- 搭建开发环境与基础架构
- 实现用户登录功能
- 开发景区地图展示功能
- 实现基础的人流数据展示

### 10.2 第二阶段：核心功能 (3 周)

- 开发路径规划功能
- 实现热力图展示
- 开发景区信息查询功能
- 完善数据模型

### 10.3 第三阶段：管理功能与优化 (2 周)

- 开发管理员功能模块
- 优化路径规划算法
- 性能优化与 bug 修复
- 部署测试环境

### 10.4 第四阶段：测试与完善 (1 周)

- 进行功能测试与用户体验测试
- 根据反馈进行调整优化
- 准备项目演示材料

















