/**
 * ActionDTO
 *
 * 说明：本文件定义了 ActionDTO，是 cidi-server 项目中的核心代码文件之一。
 *
 */

package org.daiso.cidi.modules.action.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActionDTO {


    private Long id;
    private Long userId;
    private Long postId;
    private String type;
    private LocalDateTime createTime;

}
