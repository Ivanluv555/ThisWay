/**
 * HotSearchDTO
 *
 * 说明：本文件定义了 HotSearchDTO，是 cidi-server 项目中的核心代码文件之一。
 *
 */

package org.daiso.cidi.modules.hotsearch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HotSearchDTO {


    private Integer id;
    private String keyword;
    private Integer hotValue;
    private Integer isActive;

}
