-- spider 采集景点基础信息表
-- 说明：
-- 1) 使用 spider_ 前缀，便于与业务表区分
-- 2) 唯一键 (sight_id, district_id) 用于 upsert 去重
-- 3) 当前由 cidi-model 的 /spots/sync 采集流程写入（待接入真实 MySQL 逻辑）

DROP TABLE IF EXISTS `spider_regional_scenic_spots`;
CREATE TABLE `spider_regional_scenic_spots` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID，自增',
  `sight_id` BIGINT NOT NULL COMMENT '携程景点ID（items.id）',
  `district_id` INT NOT NULL COMMENT '携程地区ID（从URL解析，如 /sight/158/xxxx.html）',
  `keyword` VARCHAR(64) NOT NULL COMMENT '同步任务的地点关键词（如：重庆）',
  `code` VARCHAR(64) DEFAULT NULL COMMENT '景点编码（items.code）',
  `name` VARCHAR(255) NOT NULL COMMENT '景点名称（items.word）',
  `url` VARCHAR(500) NOT NULL COMMENT '景点详情URL',
  `image_url` VARCHAR(500) DEFAULT NULL COMMENT '景点封面图URL（items.imageUrl）',
  `district_name` VARCHAR(128) DEFAULT NULL COMMENT '地区名称（items.districtName）',
  `e_name` VARCHAR(255) DEFAULT NULL COMMENT '英文名（items.eName）',
  `comment_score` DECIMAL(3,1) DEFAULT NULL COMMENT '评分（items.commentScore）',
  `comment_count` INT DEFAULT NULL COMMENT '评论数（items.commentCount）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sight_district` (`sight_id`, `district_id`),
  KEY `idx_district_id` (`district_id`),
  KEY `idx_keyword` (`keyword`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='spider采集的地区景点基础信息表';
