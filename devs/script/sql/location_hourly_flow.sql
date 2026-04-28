CREATE TABLE `location_hourly_flow` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `location_id` bigint(20) NOT NULL COMMENT '地点ID（外键，关联 locations.id）',
  `record_hour` datetime NOT NULL COMMENT '数据所属小时（精确到整点，如 2026-04-05 14:00:00）',
  `visitor_count` int(11) NOT NULL COMMENT '该小时实际/模拟人流量（人次）',
  `congestion_index` decimal(4,2) DEFAULT NULL COMMENT '拥挤指数 CI（0~10）',
  `data_source` varchar(16) NOT NULL DEFAULT 'simulated' COMMENT '数据来源：simulated=模拟, real=实际采集',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录插入时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_location_hour` (`location_id`,`record_hour`),
  KEY `idx_record_hour` (`record_hour`),
  CONSTRAINT `fk_lhf_location` FOREIGN KEY (`location_id`) REFERENCES `locations` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=282 DEFAULT CHARSET=utf8mb4 COMMENT='地点历史每小时人流量记录表（每小时写入21条，每景点一条）';