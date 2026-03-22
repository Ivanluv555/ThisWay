package org.daiso.cidi.modules.location.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationDTO {


    private Long id;
    private String title;
    private String address;
    private String description;
    private Integer heatScore;
    private String category;
    private BigDecimal rating;
    private BigDecimal latitude;
    private BigDecimal longitude;

}
