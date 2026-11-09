package org.daiso.cidi.modules.userfollow.entity;

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
 * 用户之间的有向关注关系，映射到 {@code user_follows} 表。
 *
 * <p>{@code followerId} 是发起关注的用户，{@code followedId} 是被关注用户，{@code createTime} 为建立关系的时间。
 *
 * <p>两个用户 ID 按标量存储；当前映射未定义二者组合唯一约束，专用关注流程在写入前查询已有关系。
 */
@Entity
@Table(name = "user_follows")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserFollow {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "follower_id", nullable = false)
    private Long followerId;

    @Column(name = "followed_id", nullable = false)
    private Long followedId;

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

}
