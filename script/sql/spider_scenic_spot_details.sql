-- spider 景点详情表
-- 说明：
-- 1) 使用 spider_ 前缀，便于与业务表区分
-- 2) 唯一键 (sight_id, district_id) 用于 upsert 去重
-- 3) 不存 qa、comment_summary、comments 等评论/问答内容

DROP TABLE IF EXISTS `spider_scenic_spot_details`;
CREATE TABLE `spider_scenic_spot_details` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID，自增',
  `sight_id` BIGINT NOT NULL COMMENT '携程景点ID（items.id）',
  `district_id` INT NOT NULL COMMENT '携程地区ID（从URL解析，如 /sight/158/xxxx.html）',
  `source_keyword` VARCHAR(64) NOT NULL COMMENT '同步任务关键词（如：重庆）',
  `spot_name` VARCHAR(255) NOT NULL COMMENT '景点名称',
  `page_title` VARCHAR(500) DEFAULT NULL COMMENT '页面标题',
  `description` TEXT DEFAULT NULL COMMENT '页面描述',
  `keywords` TEXT DEFAULT NULL COMMENT '页面 keywords',
  `heat_score` DECIMAL(4,1) DEFAULT NULL COMMENT '热度分数',
  `review_score` VARCHAR(32) DEFAULT NULL COMMENT '点评分数文本，如 4.6/5',
  `review_count` VARCHAR(32) DEFAULT NULL COMMENT '点评数量文本，如 1.6万条点评',
  `rank_text` VARCHAR(255) DEFAULT NULL COMMENT '榜单/排名文本',
  `address` VARCHAR(500) DEFAULT NULL COMMENT '地址',
  `open_time` VARCHAR(255) DEFAULT NULL COMMENT '开放时间',
  `image_origin_urls` JSON DEFAULT NULL COMMENT '原图 URL 列表',
  `image_thumbnail_urls` JSON DEFAULT NULL COMMENT '缩略图 URL 列表',
  `image_declared_total` INT DEFAULT NULL COMMENT '页面声明图片总数',
  `image_fetched_preview_count` INT DEFAULT NULL COMMENT '已抓取预览图片数量',
  `detail_modules_json` JSON DEFAULT NULL COMMENT '详情模块内容（介绍、政策、服务设施、电话等）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sight_district` (`sight_id`, `district_id`),
  KEY `idx_district_id` (`district_id`),
  KEY `idx_source_keyword` (`source_keyword`),
  KEY `idx_updated_at` (`updated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='spider采集的景点详情表';