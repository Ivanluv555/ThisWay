/**
 * FeedbackImage
 *
 * 说明：本文件定义了 FeedbackImage，是 cidi-server 项目中的核心代码文件之一。
 *
 */

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
