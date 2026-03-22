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
