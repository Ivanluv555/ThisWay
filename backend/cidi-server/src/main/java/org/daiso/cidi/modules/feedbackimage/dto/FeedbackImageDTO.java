/**
 * FeedbackImageDTO
 *
 * 说明：本文件定义了 FeedbackImageDTO，是 cidi-server 项目中的核心代码文件之一。
 *
 */

package org.daiso.cidi.modules.feedbackimage.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackImageDTO {


    private Long id;
    private Long feedbackId;
    private String imageUrl;

}
