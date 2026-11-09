package org.daiso.cidi.modules.spottrafficprediction.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 景点客流预测的数据传输模型，包含景点 ID、预测时刻、客流指标和写入时间。
 *
 * <p>保留人数、拥挤等级、趋势、交通等级及密度字段；当前预测控制器直接收发实体，此 DTO 尚未接入该调用链。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpotTrafficPredictionDTO {

    private Long id;
    private Long spotId;
    private LocalDateTime predictTime;
    private Integer visitorCount;
    private Integer congestionLevel;
    private String trend;
    private Integer trafficLevel;
    private BigDecimal crowdDensity;
    private LocalDateTime createTime;
}
