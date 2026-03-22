package org.daiso.cidi.modules.searchhistory.dto;

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
    private Integer isDeleted;
    private LocalDateTime createTime;

}
