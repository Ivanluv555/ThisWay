CREATE TABLE `location_transitions` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `source_location_id` bigint(20) NOT NULL COMMENT '源景点ID',
  `target_location_id` bigint(20) NOT NULL COMMENT '目标景点ID',
  `predict_hour` datetime NOT NULL COMMENT '预测时间（小时粒度）',
  `transition_probability` decimal(6,4) NOT NULL COMMENT '转移概率 (0-1)',
  `expected_users` int(11) NOT NULL COMMENT '预期转移人数',
  `coupon_amount` decimal(6,2) DEFAULT '0.00' COMMENT '推荐优惠券金额',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '数据生成时间',
  PRIMARY KEY (`id`),
  KEY `idx_source_predict` (`source_location_id`,`predict_hour`),
  KEY `idx_predict_time` (`predict_hour`),
  KEY `target_location_id` (`target_location_id`),
  CONSTRAINT `location_transitions_ibfk_1` FOREIGN KEY (`source_location_id`) REFERENCES `locations` (`id`) ON DELETE CASCADE,
  CONSTRAINT `location_transitions_ibfk_2` FOREIGN KEY (`target_location_id`) REFERENCES `locations` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='景点间人流转移预测表';