package org.daiso.cidi.modules.category.dto;

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
public class CategoryDTO {


    private Integer id;
    private String name;
    private String iconUrl;
    private Integer sortOrder;

}
