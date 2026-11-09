package org.daiso.cidi.modules.feedbackimage.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 反馈附件图片元数据的 JPA 持久化实体，映射到 {@code feedback_images} 表。
 *
 * <p>通过 feedbackId 标识所属反馈，imageUrl 保存附件图片的访问地址。
 *
 * <p>以标量 ID 关联反馈，当前实体未配置 JPA 关联或级联操作。
 */
@Entity
@Table(name = "feedback_images")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackImage {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "feedback_id", nullable = false)
    private Long feedbackId;

    @Column(name = "image_url", nullable = false, length = 255)
    private String imageUrl;

}
