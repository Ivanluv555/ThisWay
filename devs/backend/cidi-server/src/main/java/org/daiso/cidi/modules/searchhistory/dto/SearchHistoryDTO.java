/**
 * SearchHistoryDTO
 *
 * 说明：本文件定义了 SearchHistoryDTO，是 cidi-server 项目中的核心代码文件之一。
 *
 */

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
