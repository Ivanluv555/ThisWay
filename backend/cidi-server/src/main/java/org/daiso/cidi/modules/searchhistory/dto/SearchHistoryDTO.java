package org.daiso.cidi.modules.searchhistory.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户搜索历史的数据传输模型，包含对应记录的标识及业务字段。
 *
 * <p>通过 userId 标识搜索用户，记录关键词、创建时间和删除标记 isDeleted。
 *
 * <p>当前 {@code SearchHistoryController} 的 CRUD 接口直接收发实体，此 DTO 尚未接入该调用链。
 */
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
