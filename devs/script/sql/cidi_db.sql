-- MySQL dump 10.13  Distrib 5.7.24, for Linux (x86_64)
--
-- Host: 47.111.180.134    Database: cidi-db
-- ------------------------------------------------------
-- Server version	5.7.44

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `actions`
--

DROP TABLE IF EXISTS `actions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `actions` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint(20) NOT NULL COMMENT '操作人ID (外键)',
  `post_id` bigint(20) NOT NULL COMMENT '被操作帖子ID (外键)',
  `type` enum('like','collect') NOT NULL COMMENT '行为类型',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_post_type` (`user_id`,`post_id`,`type`),
  KEY `post_id` (`post_id`),
  CONSTRAINT `actions_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `actions_ibfk_2` FOREIGN KEY (`post_id`) REFERENCES `posts` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COMMENT='点赞与收藏流水表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `admin`
--

DROP TABLE IF EXISTS `admin`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `admin` (
  `adminid` int(11) NOT NULL AUTO_INCREMENT COMMENT '管理员ID',
  `job` varchar(255) DEFAULT NULL COMMENT '职责',
  `username` varchar(32) NOT NULL COMMENT '管理员账号', 
  `create_time` datetime(6) NOT NULL COMMENT '创建时间',
  `password` varchar(128) NOT NULL COMMENT '管理员密码(建议存哈希)',
  `role` varchar(16) NOT NULL COMMENT '角色标识',
  PRIMARY KEY (`adminid`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COMMENT='管理员账户';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `categories`
--

DROP TABLE IF EXISTS `categories`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `categories` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '分类主键ID',
  `name` varchar(32) NOT NULL COMMENT '分类名称',
  `sort_order` int(11) NOT NULL DEFAULT '0' COMMENT '排序权重',
  `is_active` tinyint(4) NOT NULL DEFAULT '1' COMMENT '是否启用(1:启用, 0:隐藏)',
  `icon_url` varchar(255) DEFAULT NULL COMMENT '分类图标URL',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COMMENT='首页动态分类配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `comments`
--

DROP TABLE IF EXISTS `comments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `comments` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `post_id` bigint(20) NOT NULL COMMENT '帖子ID (外键)',
  `user_id` bigint(20) NOT NULL COMMENT '评论人ID (外键)',
  `content` varchar(512) NOT NULL COMMENT '评论内容',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评论时间',
  `parent_id` bigint(20) DEFAULT NULL COMMENT '父评论ID(用于回复)',
  PRIMARY KEY (`id`),
  KEY `post_id` (`post_id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `comments_ibfk_1` FOREIGN KEY (`post_id`) REFERENCES `posts` (`id`) ON DELETE CASCADE,
  CONSTRAINT `comments_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COMMENT='帖子留言评论表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `feedback_images`
--

DROP TABLE IF EXISTS `feedback_images`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `feedback_images` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `feedback_id` bigint(20) NOT NULL COMMENT '归属反馈ID (外键)',
  `image_url` varchar(255) NOT NULL COMMENT '图片外链',
  PRIMARY KEY (`id`),
  KEY `feedback_id` (`feedback_id`),
  CONSTRAINT `feedback_images_ibfk_1` FOREIGN KEY (`feedback_id`) REFERENCES `feedbacks` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COMMENT='意见反馈附带截图表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `feedbacks`
--

DROP TABLE IF EXISTS `feedbacks`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `feedbacks` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint(20) DEFAULT NULL COMMENT '提交人ID(允许未登录)',
  `content` text NOT NULL COMMENT '反馈内容',
  `contact_info` varchar(64) DEFAULT NULL COMMENT '联系方式',
  `status` int(11) NOT NULL COMMENT '反馈状态',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `feedbacks_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COMMENT='用户意见反馈表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `hot_searches`
--

DROP TABLE IF EXISTS `hot_searches`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `hot_searches` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `keyword` varchar(64) NOT NULL COMMENT '热门关键词',
  `hot_value` int(11) NOT NULL DEFAULT '0' COMMENT '热度值基数',
  `is_active` tinyint(4) NOT NULL DEFAULT '1' COMMENT '是否展示',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COMMENT='热门搜索配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `location_images`
--

DROP TABLE IF EXISTS `location_images`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `location_images` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `location_id` bigint(20) NOT NULL COMMENT '归属地点ID (外键)',
  `image_url` varchar(255) NOT NULL COMMENT '图片外链',
  `sort_order` int(11) NOT NULL DEFAULT '0' COMMENT '显示顺序',
  `is_cover` int(11) NOT NULL COMMENT '是否封面图(1:是,0:否)',
  PRIMARY KEY (`id`),
  KEY `location_id` (`location_id`),
  CONSTRAINT `location_images_ibfk_1` FOREIGN KEY (`location_id`) REFERENCES `locations` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COMMENT='地点详情轮播图片表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `locations`
--

DROP TABLE IF EXISTS `locations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `locations` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '地点主键ID',
  `title` varchar(128) NOT NULL COMMENT '地点名称',
  `latitude` decimal(10,6) NOT NULL COMMENT '纬度',
  `longitude` decimal(10,6) NOT NULL COMMENT '经度',
  `address` varchar(255) NOT NULL COMMENT '详细地址',
  `description` text COMMENT '景点简介',
  `heat_score` int(11) NOT NULL DEFAULT '0' COMMENT '热度评分(用于地图显示)',
  `category` varchar(32) DEFAULT NULL COMMENT '地点分类',
  `rating` decimal(2,1) NOT NULL COMMENT '综合评分',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COMMENT='地图景点信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `post_images`
--

DROP TABLE IF EXISTS `post_images`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `post_images` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `post_id` bigint(20) NOT NULL COMMENT '归属帖子ID (外键)',
  `image_url` varchar(255) NOT NULL COMMENT '图片外链',
  `sort_order` int(11) NOT NULL DEFAULT '0' COMMENT '显示顺序',
  `height` int(11) DEFAULT NULL COMMENT '图片高度(px)',
  `width` int(11) DEFAULT NULL COMMENT '图片宽度(px)',
  PRIMARY KEY (`id`),
  KEY `post_id` (`post_id`),
  CONSTRAINT `post_images_ibfk_1` FOREIGN KEY (`post_id`) REFERENCES `posts` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COMMENT='帖子九宫格图片表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `posts`
--

DROP TABLE IF EXISTS `posts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `posts` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '帖子主键ID',
  `user_id` bigint(20) NOT NULL COMMENT '发帖人ID (外键)',
  `category_id` int(11) NOT NULL COMMENT '分类ID (外键)',
  `location_id` bigint(20) DEFAULT NULL COMMENT '关联地点ID (外键, 替代原 location_name)',
  `title` varchar(128) NOT NULL COMMENT '帖子标题',
  `content` text NOT NULL COMMENT '帖子正文',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
  `like_count` int(11) NOT NULL COMMENT '点赞数',
  `view_count` int(11) NOT NULL COMMENT '浏览量',
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  KEY `category_id` (`category_id`),
  KEY `location_id` (`location_id`),
  CONSTRAINT `posts_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `posts_ibfk_2` FOREIGN KEY (`category_id`) REFERENCES `categories` (`id`),
  CONSTRAINT `posts_ibfk_3` FOREIGN KEY (`location_id`) REFERENCES `locations` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COMMENT='图文动态主表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `search_history`
--

DROP TABLE IF EXISTS `search_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `search_history` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint(20) NOT NULL COMMENT '搜索人ID (外键)',
  `keyword` varchar(64) NOT NULL COMMENT '搜索关键词',
  `is_deleted` tinyint(4) NOT NULL DEFAULT '0' COMMENT '是否逻辑删除(0:否, 1:是)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '搜索时间',
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `search_history_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COMMENT='用户搜索历史记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `spot_traffic_predictions`
--

DROP TABLE IF EXISTS `spot_traffic_predictions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `spot_traffic_predictions` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `spot_id` bigint(20) NOT NULL COMMENT '地点ID (外键)',
  `predict_time` datetime NOT NULL COMMENT '预测目标时间',
  `visitor_count` int(11) NOT NULL COMMENT '预测人流量',
  `congestion_level` tinyint(4) NOT NULL COMMENT '拥挤等级(0:舒适, 1:适中, 2:拥挤, 3:爆满)',
  `trend` varchar(16) NOT NULL COMMENT '趋势(上升/下降)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '数据生成时间',
  `crowd_density` decimal(5,2) DEFAULT NULL COMMENT '人群密度指数',
  `traffic_level` int(11) NOT NULL COMMENT '交通拥堵等级',
  PRIMARY KEY (`id`),
  KEY `spot_id` (`spot_id`),
  CONSTRAINT `spot_traffic_predictions_ibfk_1` FOREIGN KEY (`spot_id`) REFERENCES `locations` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COMMENT='景点人流量AI预测表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_follows`
--

DROP TABLE IF EXISTS `user_follows`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `user_follows` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `follower_id` bigint(20) NOT NULL COMMENT '粉丝用户ID (外键)',
  `followed_id` bigint(20) NOT NULL COMMENT '被关注者用户ID (外键)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '关注时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_follow` (`follower_id`,`followed_id`),
  KEY `followed_id` (`followed_id`),
  CONSTRAINT `user_follows_ibfk_1` FOREIGN KEY (`follower_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `user_follows_ibfk_2` FOREIGN KEY (`followed_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COMMENT='用户关注/粉丝关系表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_settings`
--

DROP TABLE IF EXISTS `user_settings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `user_settings` (
  `user_id` bigint(20) NOT NULL COMMENT '用户ID (主键&外键)',
  `is_private_account` tinyint(4) NOT NULL DEFAULT '0' COMMENT '私密账号开关(1:开, 0:关)',
  `push_notifications` tinyint(4) NOT NULL DEFAULT '1' COMMENT '推送通知开关',
  `theme_color` varchar(32) NOT NULL DEFAULT 'system' COMMENT '主题颜色偏好(light/dark/system)',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  PRIMARY KEY (`user_id`),
  CONSTRAINT `user_settings_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户系统偏好设置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `users` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '用户主键ID',
  `openid` varchar(64) NOT NULL COMMENT '微信用户唯一标识',
  `username` varchar(64) NOT NULL COMMENT '用户昵称',
  `avatar_url` varchar(255) DEFAULT NULL COMMENT '用户头像URL',
  `bio` varchar(255) DEFAULT NULL COMMENT '个人简介/个性签名',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  `last_login_time` datetime(6) DEFAULT NULL COMMENT '最近登录时间',
  `status` int(11) NOT NULL COMMENT '账号状态(1:正常,0:禁用)',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_openid` (`openid`),
  UNIQUE KEY `UKdu5v5sr43g5bfnji4vb8hg5s3` (`phone`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COMMENT='用户基本信息表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-03-22 16:24:08
