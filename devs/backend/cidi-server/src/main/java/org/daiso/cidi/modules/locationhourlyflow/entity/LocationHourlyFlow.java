package org.daiso.cidi.modules.locationhourlyflow.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;

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
