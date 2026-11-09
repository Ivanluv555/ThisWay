package org.daiso.cidi.modules.feedback.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户反馈的数据传输模型，包含对应记录的标识及业务字段。
 *
 * <p>保存反馈正文、可选的提交用户 userId、联系方式、处理状态和创建时间。
 *
 * <p>当前 {@code FeedbackController} 的 CRUD 接口直接收发实体，此 DTO 尚未接入该调用链。
 */
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
