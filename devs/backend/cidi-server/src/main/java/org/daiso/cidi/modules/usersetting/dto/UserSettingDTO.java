/**
 * UserSettingDTO
 *
 * 说明：本文件定义了 UserSettingDTO，是 cidi-server 项目中的核心代码文件之一。
 *
 */

package org.daiso.cidi.modules.usersetting.dto;

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


    private Long userId;
    private Integer isPrivateAccount;
    private Integer pushNotifications;
    private String themeColor;
    private LocalDateTime updateTime;

}
