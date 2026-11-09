package org.daiso.cidi.modules.locationtransition.entity;

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
 * 景点间人流转移预测实体，映射到 {@code location_transitions} 表。
 *
 * <p>通过 sourceLocationId 和 targetLocationId 表示有向转移，predictHour 标识预测时刻，保存转移概率与预计人数。
 *
 * <p>可选 couponAmount 表示优惠金额，当前展示 DTO 未输出该字段；批量接口在 createTime 缺失时补齐写入时间。
 */
@Entity
@Table(name = "location_transitions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationTransition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_location_id", nullable = false)
    private Long sourceLocationId;

    @Column(name = "target_location_id", nullable = false)
    private Long targetLocationId;

    @Column(name = "predict_hour", nullable = false)
    private LocalDateTime predictHour;

    @Column(name = "transition_probability", nullable = false, precision = 6, scale = 4)
    private BigDecimal transitionProbability;

    @Column(name = "expected_users", nullable = false)
    private Integer expectedUsers;

    @Column(name = "coupon_amount", precision = 6, scale = 2)
    private BigDecimal couponAmount;

    @Column(name = "create_time")
    private LocalDateTime createTime;
}
