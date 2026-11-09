package org.daiso.cidi.modules.usersetting.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户偏好设置的数据传输模型，按 userId 标识设置所属用户。
 *
 * <p>包含私密账号、推送通知、主题和更新时间字段；当前设置接口直接使用 UserSetting 实体，此 DTO 尚未接入调用链。
 */
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
