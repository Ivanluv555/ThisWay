/**
 * WechatBindPhoneRequest
 *
 * 说明：本文件定义了 WechatBindPhoneRequest，是 cidi-server 项目中的核心代码文件之一。
 *
 */

package org.daiso.cidi.modules.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record WechatBindPhoneRequest(
        @NotNull(message = "userId 不能为空")
        Long userId,
        @NotBlank(message = "code 不能为空")
        String code
) {
}
