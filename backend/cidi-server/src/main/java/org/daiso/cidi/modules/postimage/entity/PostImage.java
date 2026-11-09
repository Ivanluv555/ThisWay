package org.daiso.cidi.modules.postimage.entity;

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
 * 帖子图片元数据的 JPA 持久化实体，映射到 {@code post_images} 表。
 *
 * <p>通过 postId 关联帖子，保存图片 URL、可选宽高和用于展示的 sortOrder。
 *
 * <p>图片内容通过 URL 引用；当前查询未按 sortOrder 自动排序。
 */
@Entity
@Table(name = "post_images")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostImage {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "image_url", nullable = false, length = 255)
    private String imageUrl;

    @Column
    private Integer width;

    @Column
    private Integer height;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

}
