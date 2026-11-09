package org.daiso.cidi.modules.user.dto;

import java.time.LocalDateTime;

/**
 * 微信登录成功响应，携带本地访问令牌、到期展示时间和用户资料。
 *
 * <p>{@code expiresAt} 以服务端本地登录时间加配置有效期的整秒数计算，JWT 自身的 exp 声明由令牌工具独立生成。
 *
 * @param token 以本地用户 ID 为 subject 的 JWT 字符串
 * @param expiresAt 服务端计算的本地到期时间，不携带时区偏移
 * @param wechatEnv 配置中的小程序环境标识
 * @param user 已保存的本地用户资料
 */
public record WechatLoginResponse(
        String token,
        LocalDateTime expiresAt,
        String wechatEnv,
        UserDTO user
) {
}
