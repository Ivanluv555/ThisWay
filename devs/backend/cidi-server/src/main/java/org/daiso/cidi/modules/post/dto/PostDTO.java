/**
 * PostDTO
 *
 * 说明：本文件定义了 PostDTO，是 cidi-server 项目中的核心代码文件之一。
 *
 */

package org.daiso.cidi.modules.post.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
