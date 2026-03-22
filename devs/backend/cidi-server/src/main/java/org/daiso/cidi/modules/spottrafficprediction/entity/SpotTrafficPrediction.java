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
