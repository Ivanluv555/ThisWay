package org.daiso.cidi.core.error;

import java.time.LocalDateTime;

/**
 * REST 接口统一的错误响应，由 {@link GlobalExceptionHandler} 创建。
 *
 * <p>将 HTTP 状态与业务错误码分开表达，同时提供服务端发生时间、提示和请求 URI，便于调用方识别失败请求。
 *
 * @param timestamp 服务端本地时间，不携带时区偏移
 * @param status HTTP 状态码
 * @param code 业务错误码，例如 {@code REQ-404}
 * @param message 面向调用方的错误提示或校验详情
 * @param path 触发错误的请求 URI，不包含查询字符串
 */
public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String code,
        String message,
        String path
) {
}
