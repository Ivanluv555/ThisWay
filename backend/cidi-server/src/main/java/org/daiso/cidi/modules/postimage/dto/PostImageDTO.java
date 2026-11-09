package org.daiso.cidi.modules.postimage.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 帖子图片元数据的数据传输模型，包含对应记录的标识及业务字段。
 *
 * <p>通过 postId 关联帖子，保存图片 URL、可选宽高和用于展示的 sortOrder。
 *
 * <p>当前 {@code PostImageController} 的 CRUD 接口直接收发实体，此 DTO 尚未接入该调用链。
 */
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
