package org.daiso.cidi.modules.hotsearch.dto;

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
public class HotSearchDTO {


    private Integer id;
    private String keyword;
    private Integer scount;
    private LocalDateTime updateTime;

}
