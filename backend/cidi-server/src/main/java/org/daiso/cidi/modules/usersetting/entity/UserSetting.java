package org.daiso.cidi.modules.usersetting.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户偏好设置实体，映射到 {@code user_settings} 表，以用户 ID 作为主键。
 *
 * <p>记录私密账号标记、推送通知标记、主题颜色和最后更新时间；每个用户 ID 对应一条设置。
 *
 * <p>专用设置服务的默认值为公开账号（0）、开启推送（1）和跟随系统主题（system）。
 */
@Entity
@Table(name = "user_settings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSetting {


    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "is_private_account", nullable = false)
    private Integer isPrivateAccount;

    @Column(name = "push_notifications", nullable = false)
    private Integer pushNotifications;

    @Column(name = "theme_color", nullable = false, length = 32)
    private String themeColor;

    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

}
