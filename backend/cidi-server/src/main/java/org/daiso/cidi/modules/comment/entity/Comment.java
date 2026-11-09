package org.daiso.cidi.modules.comment.entity;

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
 * 帖子评论的 JPA 持久化实体，映射到 {@code comments} 表。
 *
 * <p>通过 postId 关联帖子、userId 标识评论作者；可选的 parentId 表示被回复的评论。
 *
 * <p>关联字段均保存为标量 ID，当前模块未组装回复树，也未校验父评论与帖子的归属关系。
 */
@Entity
@Table(name = "comments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Comment {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(nullable = false, length = 512)
    private String content;

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

}
