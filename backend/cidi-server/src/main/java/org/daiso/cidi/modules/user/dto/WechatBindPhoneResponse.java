package org.daiso.cidi.modules.user.dto;

/**
 * 微信手机号授权绑定结果。
 *
 * <p>{@code newlyBound} 表示此次号码与绑定前的号码是否不同，因此首次绑定或更换号码为 true，重复绑定同一号码为 false。
 *
 * @param userId 已完成绑定的本地用户 ID
 * @param phone 微信返回并保存的手机号
 * @param newlyBound 本次是否改变绑定号码
 */
public record WechatBindPhoneResponse(
        Long userId,
        String phone,
        boolean newlyBound
) {
}
