/**
 * ErrorResponse
 *
 * 说明：本文件定义了 ErrorResponse，是 cidi-server 项目中的核心代码文件之一。
 *
 */

package org.daiso.cidi.core.error;

import java.time.LocalDateTime;

public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String code,
        String message,
        String path
) {
}
