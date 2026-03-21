package org.daiso.cidi.core.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    INTERNAL_SERVER_ERROR("SYS-500", "服务器内部错误", HttpStatus.INTERNAL_SERVER_ERROR),

    BAD_REQUEST("REQ-400", "请求参数错误", HttpStatus.BAD_REQUEST),
    VALIDATION_FAILED("REQ-422", "请求参数校验失败", HttpStatus.UNPROCESSABLE_ENTITY),
    UNAUTHORIZED("REQ-401", "未授权访问", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("REQ-403", "没有权限访问该资源", HttpStatus.FORBIDDEN),
    RESOURCE_NOT_FOUND("REQ-404", "资源不存在", HttpStatus.NOT_FOUND),
    METHOD_NOT_ALLOWED("REQ-405", "请求方法不被允许", HttpStatus.METHOD_NOT_ALLOWED),
    UNSUPPORTED_MEDIA_TYPE("REQ-415", "不支持的请求体类型", HttpStatus.UNSUPPORTED_MEDIA_TYPE),

    WECHAT_API_ERROR("WX-502", "微信接口调用失败", HttpStatus.BAD_GATEWAY),
    WECHAT_IDENTIFIER_MISSING("WX-503", "微信接口未返回可用标识", HttpStatus.BAD_GATEWAY),
    WECHAT_CONFIG_MISSING("WX-504", "微信配置缺失", HttpStatus.INTERNAL_SERVER_ERROR),
    WECHAT_INVALID_CODE("WX-505", "微信登录凭证无效", HttpStatus.BAD_REQUEST),
    WECHAT_PHONE_NUMBER_MISSING("WX-506", "微信接口未返回手机号", HttpStatus.BAD_GATEWAY),
    PHONE_ALREADY_BOUND("USR-409", "手机号已绑定其他用户", HttpStatus.CONFLICT);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
