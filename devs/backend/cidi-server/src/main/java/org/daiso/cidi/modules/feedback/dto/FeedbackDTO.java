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
