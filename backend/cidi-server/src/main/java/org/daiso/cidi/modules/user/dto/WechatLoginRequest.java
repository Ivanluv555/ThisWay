/**
 * WechatLoginRequest
 *
 * 说明：本文件定义了 WechatLoginRequest，是 cidi-server 项目中的核心代码文件之一。
 *
 */

package org.daiso.cidi.modules.user.dto;

import jakarta.validation.constraints.NotBlank;

public record WechatLoginRequest(
        @NotBlank(message = "code 不能为空")
        String code,
        String nickname,
        String avatarUrl
) {
}
