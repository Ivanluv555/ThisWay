package org.daiso.cidi.modules.post.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 社区帖子的 JPA 持久化实体，映射到 {@code posts} 表。
 *
 * <p>通过 userId 标识作者、categoryId 标识分类，可选的 locationId 将帖子关联到景点。
 *
 * <p>正文与浏览量、点赞量一起持久化；当前计数由写入方提供，未与互动记录自动联动。
 */
@Entity
@Table(name = "posts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Post {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "category_id", nullable = false)
    private Integer categoryId;

    @Column(name = "location_id")
    private Long locationId;

    @Column(nullable = false, length = 128)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "view_count", nullable = false)
    private Integer viewCount;

    @Column(name = "like_count", nullable = false)
    private Integer likeCount;

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

}
