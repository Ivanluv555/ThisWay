package org.daiso.cidi.modules.post.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 社区帖子的数据传输模型，包含对应记录的标识及业务字段。
 *
 * <p>通过 userId 标识作者、categoryId 标识分类，可选的 locationId 将帖子关联到景点。
 *
 * <p>当前 {@code PostController} 的 CRUD 接口直接收发实体，此 DTO 尚未接入该调用链。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostDTO {


    private Long id;
    private Long userId;
    private Integer categoryId;
    private Long locationId;
    private String title;
    private String content;
    private Integer viewCount;
    private Integer likeCount;
    private LocalDateTime createTime;

}
