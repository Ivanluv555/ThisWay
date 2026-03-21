package org.daiso.cidi.modules.user.dto;

public record WechatBindPhoneResponse(
        Long userId,
        String phone,
        boolean newlyBound
) {
}
