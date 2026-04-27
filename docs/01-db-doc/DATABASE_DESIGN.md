# CIDI 服务端数据库设计说明书

**版本**: 1.0  
**日期**: 2026-03-11  
**状态**: 正式版

---

## 1. 引言 (Introduction)

### 1.1 编写目的
本设计说明书旨在阐述 CIDI 后端服务系统的数据库架构设计思路、逻辑结构及关键业务规则。文档旨在为开发人员、数据库管理员及系统架构师提供关于数据存储层的统一视图，确保系统在数据一致性、完整性及扩展性方面满足业务需求。

### 1.2 业务背景
CIDI 系统定位为一个集内容发布（CMS）、地理位置服务（LBS）与社交互动（Social）于一体的综合性平台。系统核心业务流围绕“人（User）—地（Location）—事（Post）”三元组展开，要求数据库能够高效处理基于位置的内容聚合、用户社交关系及多媒体数据管理。

---

## 2. 总体设计原则 (Design Principles)

本数据库设计遵循以下核心原则：

1.  **第三范式（3NF）与反范式的权衡**：
    *   基础数据实体（如用户、分类、地点）严格遵循规范化设计，消除冗余，避免数据更新异常。
    *   在特定高频读取场景（如动态列表展示），通过合理的表关联设计，而非过度冗余字段，来保证数据的一致性。

2.  **数据完整性约束**：
    *   利用数据库引擎的外键约束（Foreign Key）强制维护实体间的关联关系，杜绝“孤儿数据”。
    *   合理配置级联操作（Cascade/Set Null），自动化处理数据生命周期（如用户注销后的数据清理）。

3.  **读写分离与主从设计**：
    *   采用“主信息表”与“多媒体扩展表”分离的策略（如 `posts` 与 `post_images`），减小主表行宽，提升I/O效率，适应高并发读取。

4.  **以位置为核心的聚合**：
    *   将地理位置信息独立为核心实体 `locations`，而非作为内容的附属属性，奠定了系统 LBS 业务的扩展基础。

---

## 3. 逻辑架构设计 (Logical Architecture)

系统数据域主要划分为四大模块：用户域、内容域、位置域与交互域。

### 3.1 核心实体关系 (Core Entity Relationships)

#### 3.1.1 动态发布主表 (`posts`)
`posts` 表是业务逻辑的中枢，负责串联用户、地点与分类。

*   **设计思路**：
    *   **强关联分类**：`category_id` (NOT NULL) 强制内容分类，为内容运营与推荐算法提供基础维度。
    *   **位置解耦**：通过 `location_id` (Foreign Key) 关联 `locations` 表，替代传统的文本存储。此举消除了位置名称的冗余，并支持位置元数据（如经纬度、区域热度）的独立维护。
    *   **生命周期管理**：
        *   **用户级联**：配置 `ON DELETE CASCADE`，当 `user_id` 被删除时，自动清除其发布的动态，符合隐私合规要求。
        *   **位置容错**：配置 `ON DELETE SET NULL`，即使关联的地点 POI 被系统移除，用户发布的内容文本依然保留，仅位置属性置空，保护用户资产。

#### 3.1.2 地理位置表 (`locations`)
*   **设计思路**：作为 LBS 业务的原子单元。独立建表使得系统可以轻松扩展“地点签到”、“地点热度预测”等功能，且支持对同一地点的所有动态进行快速聚合查询。

#### 3.1.3 用户与权限 (`users`, `admin`)
*   **设计思路**：
    *   普通用户 (`users`) 与管理员 (`admin`) 物理表分离，基于两者属性差异巨大（如社交属性 vs 权限管理属性），隔离设计提升了安全性。
    *   `user_settings` 表独立存储用户偏好设置，避免污染 `users` 主表的结构，便于后续增加新的配置项。

### 3.2 辅助资源扩展 (Auxiliary Resources)

#### 3.2.1 多媒体资源分离 (`*_images`)
包括 `post_images`, `location_images`, `feedback_images`。
*   **设计思路**：采用 1:N 关系存储图片资源。将图片 URL、宽高等元数据从主表剥离，不仅支持了多图发布功能，更显著降低了主表单行数据的大小，优化了全表扫描和索引检索的性能。

### 3.3 社交与互动 (Social & Interaction)

#### 3.3.1 互动行为 (`actions`, `comments`)
*   **设计思路**：
    *   **轻重分离**：将点赞/收藏等轻量级状态（`actions`）与评论文本（`comments`）分离。`actions` 表通常作为高并发写入点，结构简单，利于后续进行缓存优化或分库分表。
    *   **关注体系**：`user_follows` 表构建有向社交图谱，支撑“关注流”内容的筛选。

#### 3.3.2 搜索与发现 (`hot_searches`, `search_history`)
*   **设计思路**：
    *   系统主动记录用户行为数据。`search_history` 用于个性化记录，`hot_searches` 用于全局热点统计。这标志着系统具备流量分发与用户意图分析的能力。

#### 3.3.3 智能分析 (`spot_traffic_predictions`)
*   **设计思路**：该表作为分析结果存储表，用于存放对特定地点流量拥堵情况的预测数据，展示了系统从单纯的 CRUD 向数据智能应用的延伸。

---

## 4. 详细表结构定义 (Detailed Table Schema)

以下为系统核心表的详细结构定义与说明。字段类型以 MySQL 8.0 为基准。

### 4.1 用户与权限域 (User & Access Domain)

#### 4.1.1 `users` - 普通用户表
存储 C 端用户的基本信息与认证凭证。
*   **关联**: 被 `posts`, `comments`, `actions` 等表引用。

| 字段名 | 类型 | 必填 | 默认值 | 说明 |
| :--- | :--- | :--- | :--- | :--- |
| `id` | BIGINT | YES | AUTO_INCREMENT | 主键ID |
| `user_id` | BIGINT | YES | NULL | 搜索人ID (外键) |
| `keyword` | VARCHAR | YES | NULL | 搜索关键词 |
| `is_deleted` | INT | YES | NULL |  |
| `create_time` | DATETIME | YES | CURRENT_TIMESTAMP | 搜索时间 |

#### 4.1.2 `admin` - 管理员表
存储后台管理人员信息，与普通用户物理隔离。

| 字段名 | 类型 | 必填 | 默认值 | 说明 |
| :--- | :--- | :--- | :--- | :--- |
| `adminid` | INT | YES | AUTO_INCREMENT | 管理员ID (PK) |
| `job` | VARCHAR | NO | NULL | 职责 |
| `username` | VARCHAR | YES | NULL | 管理员账号 |
| `create_time` | DATETIME | YES | NULL | 创建时间 |
| `password` | VARCHAR | YES | NULL | 管理员密码(建议存哈希) |
| `role` | VARCHAR | YES | NULL | 角色标识 |

#### 4.1.3 `user_settings` - 用户个性化设置
扩展表，存储非核心的用户偏好配置。

| 字段名 | 类型 | 必填 | 默认值 | 说明 |
| :--- | :--- | :--- | :--- | :--- |
| `user_id` | BIGINT | YES | NULL | 用户ID (主键&外键) |
| `is_private_account` | INT | YES | NULL |  |
| `push_notifications` | INT | YES | NULL |  |
| `theme_color` | VARCHAR | YES | 'system' | 主题颜色偏好(light/dark/system) |
| `update_time` | DATETIME | YES | CURRENT_TIMESTAMP | 最后更新时间 |

#### 4.1.4 `user_follows` - 用户关注关系
构建社交图谱的边表。

| 字段名 | 类型 | 必填 | 默认值 | 说明 |
| :--- | :--- | :--- | :--- | :--- |
| `id` | BIGINT | YES | AUTO_INCREMENT | 主键ID |
| `follower_id` | BIGINT | YES | NULL | 粉丝用户ID (外键) |
| `followed_id` | BIGINT | YES | NULL | 被关注者用户ID (外键) |
| `create_time` | DATETIME | YES | CURRENT_TIMESTAMP | 关注时间 |

### 4.2 位置服务域 (Location & LBS Domain)

#### 4.2.1 `locations` - 地点信息表 (POI)
核心 LBS 实体。

| 字段名 | 类型 | 必填 | 默认值 | 说明 |
| :--- | :--- | :--- | :--- | :--- |
| `id` | BIGINT | YES | AUTO_INCREMENT | 地点主键ID |
| `title` | VARCHAR | YES | NULL | 地点名称 |
| `latitude` | DECIMAL | YES | NULL | 纬度 |
| `longitude` | DECIMAL | YES | NULL | 经度 |
| `address` | VARCHAR | YES | NULL | 详细地址 |
| `description` | TEXT | NO | NULL | 景点简介 |
| `heat_score` | INT | YES | 0 | 热度评分(用于地图显示) |
| `category` | VARCHAR | NO | NULL | 地点分类 |
| `rating` | DECIMAL | YES | NULL | 综合评分 |

#### 4.2.2 `location_images` - 地点图集
存储地点的官方展示图或封面图。

| 字段名 | 类型 | 必填 | 默认值 | 说明 |
| :--- | :--- | :--- | :--- | :--- |
| `id` | BIGINT | YES | AUTO_INCREMENT | 主键ID |
| `location_id` | BIGINT | YES | NULL | 归属地点ID (外键) |
| `image_url` | VARCHAR | YES | NULL | 图片外链 |
| `sort_order` | INT | YES | 0 | 显示顺序 |
| `is_cover` | INT | YES | NULL | 是否封面图(1:是,0:否) |

#### 4.2.3 `spot_traffic_predictions` - 地点流量预测
分析型数据表，存储特定时间段的拥堵/流量预测结果。

| 字段名 | 类型 | 必填 | 默认值 | 说明 |
| :--- | :--- | :--- | :--- | :--- |
| `id` | BIGINT | YES | AUTO_INCREMENT |  (PK) |
| `congestion_level` | INT | YES | NULL |  |
| `create_time` | DATETIME | YES | NULL |  |
| `crowd_density` | DECIMAL | NO | NULL |  |
| `predict_time` | DATETIME | YES | NULL |  |
| `spot_id` | BIGINT | YES | NULL |  |
| `traffic_level` | INT | YES | NULL |  |
| `trend` | VARCHAR | YES | NULL |  |
| `visitor_count` | INT | YES | NULL |  |

#### 4.2.4 `spot_regions` - 景区内部划分区域

| 字段名 | 类型 | 必填 | 默认值 | 说明 |
| :--- | :--- | :--- | :--- | :--- |
| `id` | BIGINT | YES | AUTO_INCREMENT | 区域主键ID |
| `spot_id` | BIGINT | YES | NULL | 所属景点ID (对应 locations 表) |
| `title` | VARCHAR | YES | NULL | 区域名称 (如：检票口、核心观景台) |
| `area` | DECIMAL | YES | NULL | 有效可活动面积 (㎡) - S_i |
| `max_capacity` | INT | YES | NULL | 设计最大承载量 (人) - C_i^max |
| `category` | VARCHAR | YES | NULL | 区域功能类型 (如：ENTRY, CORE, PATH 等) - τ_i |
| `shape_factor` | DECIMAL | YES | 1.00 | 空间形态系数 (封闭度折减) - κ_i |
| `bottleneck_factor` | DECIMAL | YES | 1.00 | 结构性瓶颈系数 - B_i |
| `service_throughput` | DECIMAL | NO | 0.00 | 服务吞吐量 (人/分钟) - M_i |
| `perception_weight` | DECIMAL | YES | 1.00 | 场景感知权重 - w_i |

#### 4.2.5 `location_hourly_flow` - 地点分时客流数据

| 字段名 | 类型 | 必填 | 默认值 | 说明 |
| :--- | :--- | :--- | :--- | :--- |
| `id` | BIGINT | YES | AUTO_INCREMENT | 主键ID |
| `location_id` | BIGINT | YES | NULL | 地点ID（外键，关联 locations.id） |
| `record_hour` | DATETIME | YES | NULL | 数据所属小时（精确到整点，如 2026-04-05 14:00:00） |
| `visitor_count` | INT | YES | NULL | 该小时实际/模拟人流量（人次） |
| `congestion_index` | DECIMAL | NO | NULL | 拥挤指数 CI（0~10） |
| `data_source` | VARCHAR | YES | 'simulated' | 数据来源：simulated=模拟, real=实际采集 |
| `created_at` | DATETIME | YES | CURRENT_TIMESTAMP | 记录插入时间 |

#### 4.2.6 `locations_predictions` - 地点维度客流预测

| 字段名 | 类型 | 必填 | 默认值 | 说明 |
| :--- | :--- | :--- | :--- | :--- |
| `id` | BIGINT | YES | AUTO_INCREMENT | 主键ID |
| `location_id` | BIGINT | YES | NULL | 地点ID (外键) |
| `predict_time` | DATETIME | YES | NULL | 预测目标时间 |
| `visitor_count` | INT | YES | NULL | 预测人流量 |
| `congestion_level` | INT | YES | NULL |  |
| `trend` | VARCHAR | YES | NULL | 趋势(上升/下降) |
| `create_time` | DATETIME | YES | CURRENT_TIMESTAMP | 数据生成时间 |
| `crowd_density` | DECIMAL | NO | NULL | 人群密度指数 |
| `traffic_level` | INT | YES | NULL | 交通拥堵等级 |

### 4.3 图文动态域 (Content Domain)

#### 4.3.1 `categories` - 内容分类
帖子的分类维度。

| 字段名 | 类型 | 必填 | 默认值 | 说明 |
| :--- | :--- | :--- | :--- | :--- |
| `id` | INT | YES | AUTO_INCREMENT | 分类主键ID |
| `name` | VARCHAR | YES | NULL | 分类名称 |
| `sort_order` | INT | YES | 0 | 排序权重 |
| `is_active` | INT | YES | NULL |  |
| `icon_url` | VARCHAR | NO | NULL |  |

#### 4.3.2 `posts` - 帖子主表
核心内容实体。

| 字段名 | 类型 | 必填 | 默认值 | 说明 |
| :--- | :--- | :--- | :--- | :--- |
| `id` | BIGINT | YES | AUTO_INCREMENT | 帖子主键ID |
| `user_id` | BIGINT | YES | NULL | 发帖人ID (外键) |
| `category_id` | INT | YES | NULL | 分类ID (外键) |
| `location_id` | BIGINT | NO | NULL | 关联地点ID (外键, 替代原 location_name) |
| `title` | VARCHAR | YES | NULL | 帖子标题 |
| `content` | TEXT | YES | NULL | 帖子正文 |
| `create_time` | DATETIME | YES | CURRENT_TIMESTAMP | 发布时间 |
| `like_count` | INT | YES | NULL | 点赞数 |
| `view_count` | INT | YES | NULL | 浏览量 |

#### 4.3.3 `post_images` - 帖子图片
帖子多图存储。

| 字段名 | 类型 | 必填 | 默认值 | 说明 |
| :--- | :--- | :--- | :--- | :--- |
| `id` | BIGINT | YES | AUTO_INCREMENT | 主键ID |
| `post_id` | BIGINT | YES | NULL | 归属帖子ID (外键) |
| `image_url` | VARCHAR | YES | NULL | 图片外链 |
| `sort_order` | INT | YES | 0 | 显示顺序 |
| `height` | INT | NO | NULL | 图片高度(px) |
| `width` | INT | NO | NULL | 图片宽度(px) |

### 4.4 交互与反馈域 (Interaction & Feedback Domain)

#### 4.4.1 `comments` - 评论表
用户对帖子的文本回复。

| 字段名 | 类型 | 必填 | 默认值 | 说明 |
| :--- | :--- | :--- | :--- | :--- |
| `id` | BIGINT | YES | AUTO_INCREMENT | 主键ID |
| `post_id` | BIGINT | YES | NULL | 帖子ID (外键) |
| `user_id` | BIGINT | YES | NULL | 评论人ID (外键) |
| `content` | VARCHAR | YES | NULL | 评论内容 |
| `create_time` | DATETIME | YES | CURRENT_TIMESTAMP | 评论时间 |
| `parent_id` | BIGINT | NO | NULL | 父评论ID(用于回复) |

#### 4.4.2 `actions` - 用户交互行为
记录点赞、收藏等状态。

| 字段名 | 类型 | 必填 | 默认值 | 说明 |
| :--- | :--- | :--- | :--- | :--- |
| `id` | BIGINT | YES | AUTO_INCREMENT | 主键ID |
| `user_id` | BIGINT | YES | NULL | 操作人ID (外键) |
| `post_id` | BIGINT | YES | NULL | 被操作帖子ID (外键) |
| `type` | VARCHAR | YES | NULL |  |
| `create_time` | DATETIME | YES | CURRENT_TIMESTAMP | 操作时间 |

#### 4.4.3 `feedbacks` - 用户反馈
用户意见提交。

| 字段名 | 类型 | 必填 | 默认值 | 说明 |
| :--- | :--- | :--- | :--- | :--- |
| `id` | BIGINT | YES | AUTO_INCREMENT | 主键ID |
| `user_id` | BIGINT | NO | NULL | 提交人ID(允许未登录) |
| `content` | TEXT | YES | NULL | 反馈内容 |
| `contact_info` | VARCHAR | NO | NULL | 联系方式 |
| `status` | INT | YES | NULL | 反馈状态 |
| `create_time` | DATETIME | YES | CURRENT_TIMESTAMP | 提交时间 |

#### 4.4.4 `feedback_images` - 反馈图片
反馈附带的截图或凭证。

| 字段名 | 类型 | 必填 | 默认值 | 说明 |
| :--- | :--- | :--- | :--- | :--- |
| `id` | BIGINT | YES | AUTO_INCREMENT | 主键ID |
| `feedback_id` | BIGINT | YES | NULL | 归属反馈ID (外键) |
| `image_url` | VARCHAR | YES | NULL | 图片外链 |

### 4.5 搜索与发现域 (Search & Discovery Domain)

#### 4.5.1 `hot_searches` - 热搜榜单
全站热词统计。

| 字段名 | 类型 | 必填 | 默认值 | 说明 |
| :--- | :--- | :--- | :--- | :--- |
| `id` | INT | YES | AUTO_INCREMENT | 主键ID |
| `keyword` | VARCHAR | YES | NULL | 热门关键词 |
| `hot_value` | INT | YES | 0 | 热度值基数 |
| `is_active` | INT | YES | NULL |  |

#### 4.5.2 `search_history` - 搜索历史
用户个人搜索记录。

| 字段名 | 类型 | 必填 | 默认值 | 说明 |
| :--- | :--- | :--- | :--- | :--- |
| `id` | BIGINT | YES | AUTO_INCREMENT | 主键ID |
| `user_id` | BIGINT | YES | NULL | 搜索人ID (外键) |
| `keyword` | VARCHAR | YES | NULL | 搜索关键词 |
| `is_deleted` | INT | YES | NULL |  |
| `create_time` | DATETIME | YES | CURRENT_TIMESTAMP | 搜索时间 |

---

## 5. 关键索引策略 (Indexing Strategy)

为保证查询性能，针对高频查询路径制定了以下索引策略：

1.  **用户维度查询**：在 `posts(user_id)` 建立索引，优化“个人主页”动态加载速度。
2.  **位置维度查询**：在 `posts(location_id)` 建立索引，优化“查看该地点所有动态”的 LBS 聚合查询。
3.  **分类维度查询**：在 `posts(category_id)` 建立索引，优化频道页面的内容筛选。
4.  **去重与唯一性**：
    *   `users(openid)`: 唯一索引，保证微信用户唯一性。
    *   `actions(user_id, target_id, target_type, action_type)`: 复合唯一索引，防止重复点赞。
    *   `user_follows(follower_id, followee_id)`: 复合唯一索引，防止重复关注。
