package org.daiso.cidi.modules.usersetting.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSettingDTO {


    private Long id;
    private Integer notificationEnabled;
    private Integer privacyLevel;
    private String theme;

}
