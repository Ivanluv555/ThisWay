package org.daiso.cidi.modules.feedbackimage.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 反馈附件图片元数据的数据传输模型，包含对应记录的标识及业务字段。
 *
 * <p>通过 feedbackId 标识所属反馈，imageUrl 保存附件图片的访问地址。
 *
 * <p>当前 {@code FeedbackImageController} 的 CRUD 接口直接收发实体，此 DTO 尚未接入该调用链。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackImageDTO {


    private Long id;
    private Long feedbackId;
    private String imageUrl;

}
