package org.daiso.cidi.modules.user.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 微信登录请求，供 {@code /api/auth/wechat/login} 使用。
 *
 * <p>登录 code 用于兑换微信会话；可选资料仅在非空白时覆盖本地用户资料，空值保留已有资料。
 *
 * @param code 小程序获取的登录凭证，不能为空白
 * @param nickname 可选昵称；新用户未提供时由服务生成默认昵称
 * @param avatarUrl 可选头像地址
 */
public record WechatLoginRequest(
        @NotBlank(message = "code 不能为空")
        String code,
        String nickname,
        String avatarUrl
) {
}
