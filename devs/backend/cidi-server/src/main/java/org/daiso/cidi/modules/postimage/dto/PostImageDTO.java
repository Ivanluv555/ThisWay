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
