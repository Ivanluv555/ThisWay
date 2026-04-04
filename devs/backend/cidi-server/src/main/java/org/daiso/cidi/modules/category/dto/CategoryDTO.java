/**
 * CategoryDTO
 *
 * 说明：本文件定义了 CategoryDTO，是 cidi-server 项目中的核心代码文件之一。
 *
 */

package org.daiso.cidi.modules.category.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
