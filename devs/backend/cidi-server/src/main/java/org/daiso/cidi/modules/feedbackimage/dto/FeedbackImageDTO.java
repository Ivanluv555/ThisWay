package org.daiso.cidi.modules.feedbackimage.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
