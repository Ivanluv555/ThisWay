/**
 * PostImageDTO
 *
 * 说明：本文件定义了 PostImageDTO，是 cidi-server 项目中的核心代码文件之一。
 *
 */

package org.daiso.cidi.modules.postimage.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostImageDTO {


    private Long id;
    private Long postId;
    private String imageUrl;
    private Integer width;
    private Integer height;
    private Integer sortOrder;

}
