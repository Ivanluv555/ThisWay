package org.daiso.cidi.modules.user.entity;

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
 * 小程序用户资料的 JPA 持久化实体，映射到 {@code users} 表。
 *
 * <p>以唯一 openid 关联微信身份，保存昵称、头像、简介、可选唯一手机号、状态和登录时间。
 *
 * <p>微信登录服务按 openid 查找或创建用户，新用户状态为 1；手机号授权绑定单独处理。
 */
@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String openid;

    @Column(nullable = false, length = 64)
    private String username;

    @Column(name = "avatar_url", length = 255)
    private String avatarUrl;

    @Column(length = 255)
    private String bio;

    @Column(name = "phone", unique = true, length = 20)
    private String phone;

    @Column(nullable = false)
    private Integer status;

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    @Column(name = "last_login_time")
    private LocalDateTime lastLoginTime;

}
