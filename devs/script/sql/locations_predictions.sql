CREATE TABLE `locations_predictions` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `location_id` bigint(20) NOT NULL COMMENT '地点ID (外键)',
  `predict_time` datetime NOT NULL COMMENT '预测目标时间',
  `visitor_count` int(11) NOT NULL COMMENT '预测人流量',
  `congestion_level` int(11) NOT NULL,
  `trend` varchar(16) NOT NULL COMMENT '趋势(上升/下降)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '数据生成时间',
  `crowd_density` decimal(5,2) DEFAULT NULL COMMENT '人群密度指数',
  `traffic_level` int(11) NOT NULL COMMENT '交通拥堵等级',
  PRIMARY KEY (`id`),
  KEY `spot_id` (`location_id`),
  CONSTRAINT `locations_predictions_ibfk_1` FOREIGN KEY (`location_id`) REFERENCES `locations` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='景点人流量AI预测表';