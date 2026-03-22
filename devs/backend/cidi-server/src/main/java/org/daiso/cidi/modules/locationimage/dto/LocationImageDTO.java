package org.daiso.cidi.modules.locationimage.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationImageDTO {


    private Long id;
    private Long locationId;
    private String imageUrl;
    private Integer isCover;
    private Integer sortOrder;

}
