/**
 * CidiException
 *
 * 说明：本文件定义了 CidiException，是 cidi-server 项目中的核心代码文件之一。
 *
 */

package org.daiso.cidi.core.error;

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
