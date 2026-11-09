package org.daiso.cidi.modules.admin.entity;

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
 * 管理员资料的 JPA 持久化实体，映射到 {@code admin} 表。
 *
 * <p>保存后台人员的岗位、唯一用户名、密码字段、角色字符串和创建时间，主键映射到 adminid。
 *
 * <p>password 和 role 按普通字段读写；当前模块仅维护资料，未实现密码编码或角色鉴权。
 */
@Entity
@Table(name = "admin")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Admin {


    @Id
    @Column(name = "adminid")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = 255)
    private String job;

    @Column(nullable = false, unique = true, length = 32)
    private String username;

    @Column(nullable = false, length = 128)
    private String password;

    @Column(nullable = false, length = 16)
    private String role;

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

}
