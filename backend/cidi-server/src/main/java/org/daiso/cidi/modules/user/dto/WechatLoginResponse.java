/**
 * WechatLoginResponse
 *
 * 说明：本文件定义了 WechatLoginResponse，是 cidi-server 项目中的核心代码文件之一。
 *
 */

package org.daiso.cidi.modules.user.dto;

import java.time.LocalDateTime;

public record WechatLoginResponse(
        String token,
        LocalDateTime expiresAt,
        String wechatEnv,
        UserDTO user
) {
}
