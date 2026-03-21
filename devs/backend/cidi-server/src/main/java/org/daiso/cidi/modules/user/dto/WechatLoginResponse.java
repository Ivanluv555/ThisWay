package org.daiso.cidi.modules.user.dto;

import java.time.LocalDateTime;

public record WechatLoginResponse(
        String token,
        LocalDateTime expiresAt,
        String wechatEnv,
        UserDTO user
) {
}
