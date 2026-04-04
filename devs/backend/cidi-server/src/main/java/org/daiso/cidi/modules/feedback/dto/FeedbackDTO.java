/**
 * FeedbackDTO
 *
 * 说明：本文件定义了 FeedbackDTO，是 cidi-server 项目中的核心代码文件之一。
 *
 */

package org.daiso.cidi.modules.feedback.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackDTO {


    private Long id;
    private Long userId;
    private String content;
    private String contactInfo;
    private Integer status;
    private LocalDateTime createTime;

}
