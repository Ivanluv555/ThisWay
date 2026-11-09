package org.daiso.cidi.modules.category.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 帖子分类的数据传输模型，包含对应记录的标识及业务字段。
 *
 * <p>记录分类名称、图标地址、排序值 sortOrder 和启用标记 isActive，供帖子通过 categoryId 引用。
 *
 * <p>当前 {@code CategoryController} 的 CRUD 接口直接收发实体，此 DTO 尚未接入该调用链。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDTO {


    private Integer id;
    private String name;
    private String iconUrl;
    private Integer sortOrder;
    private Integer isActive;

}
