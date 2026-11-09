package org.daiso.cidi.modules.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 微信手机号绑定请求，供 {@code /api/auth/wechat/phone/bind} 使用。
 *
 * <p>手机号由服务端使用授权 code 向微信兑换；目标用户由请求中的 userId 指定。
 *
 * @param userId 待绑定的本地用户 ID，不能为空
 * @param code 手机号授权凭证，与登录 code 用途不同，不能为空白
 */
public record WechatBindPhoneRequest(
        @NotNull(message = "userId 不能为空")
        Long userId,
        @NotBlank(message = "code 不能为空")
        String code
) {
}
