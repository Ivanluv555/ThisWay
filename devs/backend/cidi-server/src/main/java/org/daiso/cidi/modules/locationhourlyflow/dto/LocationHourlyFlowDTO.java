package org.daiso.cidi.modules.locationhourlyflow.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationHourlyFlowDTO {

    private Long id;

    private Long locationId;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime recordHour;

    private Integer visitorCount;

    private BigDecimal congestionIndex;

    @Builder.Default
    private String dataSource = "simulated";
}
