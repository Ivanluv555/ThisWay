package org.daiso.cidi.modules.locationhourlyflow.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;

/**
 * 景点小时客流历史实体，映射到 {@code location_hourly_flow} 表。
 *
 * <p>按 locationId 和 recordHour 保存人数、拥挤指数及来源标识，两字段组合具有数据库唯一约束。
 *
 * <p>dataSource 默认是 simulated；首次持久化时自动补齐 createdAt。recordHour 由写入方提供，实体不会将时间截整到小时。
 */
@Entity
@Table(
    name = "location_hourly_flow",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_location_hour",
        columnNames = {"location_id", "record_hour"}
    )
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationHourlyFlow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "location_id", nullable = false)
    private Long locationId;

    @Column(name = "record_hour", nullable = false)
    private LocalDateTime recordHour;

    @Column(name = "visitor_count", nullable = false)
    private Integer visitorCount;

    @Column(name = "congestion_index", precision = 4, scale = 2)
    private BigDecimal congestionIndex;

    @Column(name = "data_source", nullable = false, length = 16)
    @Builder.Default
    private String dataSource = "simulated";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onPersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
