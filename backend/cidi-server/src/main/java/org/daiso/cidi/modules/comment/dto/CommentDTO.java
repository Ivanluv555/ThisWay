package org.daiso.cidi.modules.comment.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 帖子评论的数据传输模型，包含对应记录的标识及业务字段。
 *
 * <p>通过 postId 关联帖子、userId 标识评论作者；可选的 parentId 表示被回复的评论。
 *
 * <p>当前 {@code CommentController} 的 CRUD 接口直接收发实体，此 DTO 尚未接入该调用链。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentDTO {


    private Long id;
    private Long postId;
    private Long userId;
    private Long parentId;
    private String content;
    private LocalDateTime createTime;

}
