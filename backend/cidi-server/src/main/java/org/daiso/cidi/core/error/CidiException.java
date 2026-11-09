package org.daiso.cidi.core.error;

/**
 * 携带 {@link ErrorCode} 的非受检业务异常，用于向 HTTP 层传递可识别的失败原因。
 *
 * <p>支持默认提示、自定义详情及原始异常原因；{@link GlobalExceptionHandler} 将错误码映射为 HTTP 状态和 {@link ErrorResponse}。
 *
 * <p>异常消息会作为错误响应的 message 返回调用方，原始 cause 不直接写入响应。
 */
public class CidiException extends RuntimeException {
    private final ErrorCode errorCode;

    public CidiException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public CidiException(ErrorCode errorCode, String detailMessage) {
        super(detailMessage);
        this.errorCode = errorCode;
    }

    public CidiException(ErrorCode errorCode, String detailMessage, Throwable cause) {
        super(detailMessage, cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
