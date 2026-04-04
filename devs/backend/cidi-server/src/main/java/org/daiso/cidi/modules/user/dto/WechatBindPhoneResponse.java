/**
 * WechatBindPhoneResponse
 *
 * 说明：本文件定义了 WechatBindPhoneResponse，是 cidi-server 项目中的核心代码文件之一。
 *
 */

package org.daiso.cidi.modules.user.dto;

public record WechatBindPhoneResponse(
        Long userId,
        String phone,
        boolean newlyBound
) {
}
