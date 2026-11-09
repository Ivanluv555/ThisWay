package org.daiso.cidi.modules.action.entity;

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
 * 帖子互动记录的 JPA 持久化实体，映射到 {@code actions} 表。
 *
 * <p>以 userId、postId 和 type 描述用户对帖子的某种操作，createTime 记录操作时间。
 *
 * <p>type 为字符串，当前代码未定义取值枚举；保存记录不会同步修改帖子的 likeCount。
 */
@Entity
@Table(name = "actions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Action {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(nullable = false, length = 16)
    private String type;

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

}
