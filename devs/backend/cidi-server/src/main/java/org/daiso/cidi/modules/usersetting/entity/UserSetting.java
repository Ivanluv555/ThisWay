package org.daiso.cidi.modules.usersetting.entity;

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
@Table(name = "user_settings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSetting {


    @Id
    @Column(name = "user_id")
    private Long id;

    @Column(name = "notification_enabled", nullable = false)
    private Integer notificationEnabled;

    @Column(name = "privacy_level", nullable = false)
    private Integer privacyLevel;

    @Column(nullable = false, length = 16)
    private String theme;

}
