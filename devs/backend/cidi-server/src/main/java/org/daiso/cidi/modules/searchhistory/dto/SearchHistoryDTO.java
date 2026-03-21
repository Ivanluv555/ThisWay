package org.daiso.cidi.modules.searchhistory.dto;

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
public class SearchHistoryDTO {


    private Long id;
    private Long userId;
    private String keyword;
    private LocalDateTime createTime;

}
