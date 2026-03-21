package org.daiso.cidi.modules.user.dto;

import jakarta.validation.constraints.NotBlank;

public record WechatLoginRequest(
        @NotBlank(message = "code 不能为空")
        String code,
        String nickname,
        String avatarUrl
) {
}
