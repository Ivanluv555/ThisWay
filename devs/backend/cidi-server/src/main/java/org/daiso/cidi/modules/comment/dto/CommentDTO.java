/**
 * CommentDTO
 *
 * 说明：本文件定义了 CommentDTO，是 cidi-server 项目中的核心代码文件之一。
 *
 */

package org.daiso.cidi.modules.comment.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
