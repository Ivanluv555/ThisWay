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
