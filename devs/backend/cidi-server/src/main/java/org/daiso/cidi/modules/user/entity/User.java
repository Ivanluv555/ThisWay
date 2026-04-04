/**
 * User
 *
 * 说明：本文件定义了 User，是 cidi-server 项目中的核心代码文件之一。
 *
 */

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
