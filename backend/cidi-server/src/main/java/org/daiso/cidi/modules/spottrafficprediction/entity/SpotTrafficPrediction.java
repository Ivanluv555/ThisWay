package org.daiso.cidi.modules.spottrafficprediction.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 景点客流预测结果实体，映射到 {@code spot_traffic_predictions} 表。
 *
 * <p>spotId 标识景点，predictTime 表示预测时刻，保存人数、拥挤等级、趋势、交通等级和可选人群密度。
 *
 * <p>predictTime 用于识别最新一轮结果，createTime 用于选取最近写入记录；各等级、趋势及密度的业务取值由外部写入方提供。
 */
@Entity
@Table(name = "spot_traffic_predictions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpotTrafficPrediction {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "spot_id", nullable = false)
    private Long spotId;

    @Column(name = "predict_time", nullable = false)
    private LocalDateTime predictTime;

    @Column(name = "visitor_count", nullable = false)
    private Integer visitorCount;

    @Column(name = "congestion_level", nullable = false)
    private Integer congestionLevel;

    @Column(nullable = false, length = 16)
    private String trend;

    @Column(name = "traffic_level", nullable = false)
    private Integer trafficLevel;

    @Column(name = "crowd_density", precision = 5, scale = 2)
    private BigDecimal crowdDensity;

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

}
