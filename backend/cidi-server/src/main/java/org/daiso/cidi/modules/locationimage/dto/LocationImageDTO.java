package org.daiso.cidi.modules.locationimage.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 景点图片元数据的数据传输模型，包含对应记录的标识及业务字段。
 *
 * <p>以 locationId 标识所属景点，保存图片 URL、封面标记 isCover 和展示顺序 sortOrder。
 *
 * <p>当前 {@code LocationImageController} 的 CRUD 接口直接收发实体，此 DTO 尚未接入该调用链。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationImageDTO {


    private Long id;
    private Long locationId;
    private String imageUrl;
    private Integer isCover;
    private Integer sortOrder;

}
