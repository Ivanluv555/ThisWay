/**
 * SpotTrafficPredictionDTO
 *
 * 说明：本文件定义了 SpotTrafficPredictionDTO，是 cidi-server 项目中的核心代码文件之一。
 *
 */

package org.daiso.cidi.modules.spottrafficprediction.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
