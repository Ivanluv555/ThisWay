package org.daiso.cidi.core.error;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.MapBindingResult;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private static final String PATH = "/api/test";

    private GlobalExceptionHandler handler;
    private LocalDateTime startedAt;

    @Mock
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        startedAt = LocalDateTime.now();
        when(request.getRequestURI()).thenReturn(PATH);
    }

    /**
     * 将业务异常的分类与具体说明转换为统一响应。
     *
     * <p>测试输入：错误码 FORBIDDEN、消息 not allowed 的 CidiException。 请求 URI 均为 /api/test。</p>
     * <p>预期结果：返回 HTTP 403、REQ-403 和消息 not allowed。 所有响应同时验证 body.status 与 HTTP 状态一致、path 为 /api/test、timestamp 在本次测试起止时间之间。</p>
     */
    @Test
    void handlesCidiExceptionUsingItsCodeAndMessage() {
        ResponseEntity<ErrorResponse> response = handler.handleCidiException(
                new CidiException(ErrorCode.FORBIDDEN, "not allowed"), request);

        assertResponse(response, ErrorCode.FORBIDDEN, "not allowed");
    }

    /**
     * 将请求参数校验失败按字段顺序合并，使调用方可定位多个无效输入。
     *
     * <p>测试输入：包含 name: must not be blank 与 age: must be positive 两个字段错误的 MethodArgumentNotValidException。 请求 URI 均为 /api/test。</p>
     * <p>预期结果：返回 HTTP 422、REQ-422，消息为 name: must not be blank; age: must be positive。 所有响应同时验证 body.status 与 HTTP 状态一致、path 为 /api/test、timestamp 在本次测试起止时间之间。</p>
     */
    @Test
    void handlesMethodArgumentNotValidExceptionWithFieldDetails() {
        BindingResult bindingResult = bindingResult(
                new FieldError("command", "name", "must not be blank"),
                new FieldError("command", "age", "must be positive"));
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(
                mock(MethodParameter.class), bindingResult);

        ResponseEntity<ErrorResponse> response = handler.handleValidationException(exception, request);

        assertResponse(response, ErrorCode.VALIDATION_FAILED,
                "name: must not be blank; age: must be positive");
    }

    /**
     * 数据绑定异常与参数校验异常采用相同的字段错误响应规范。
     *
     * <p>测试输入：包含 email: must be valid 与 name: must not be blank 两个字段错误的 BindException。 请求 URI 均为 /api/test。</p>
     * <p>预期结果：返回 HTTP 422、REQ-422，消息为 email: must be valid; name: must not be blank。 所有响应同时验证 body.status 与 HTTP 状态一致、path 为 /api/test、timestamp 在本次测试起止时间之间。</p>
     */
    @Test
    void handlesBindExceptionWithFieldDetails() {
        BindException exception = new BindException(bindingResult(
                new FieldError("command", "email", "must be valid"),
                new FieldError("command", "name", "must not be blank")));

        ResponseEntity<ErrorResponse> response = handler.handleValidationException(exception, request);

        assertResponse(response, ErrorCode.VALIDATION_FAILED,
                "email: must be valid; name: must not be blank");
    }

    /**
     * 没有可拼接的字段详情时，仍向调用方提供可读的失败说明。
     *
     * <p>测试输入：字段错误列表为空的 MethodArgumentNotValidException。 请求 URI 均为 /api/test。</p>
     * <p>预期结果：返回 HTTP 422、REQ-422 和默认消息“请求参数校验失败”。 所有响应同时验证 body.status 与 HTTP 状态一致、path 为 /api/test、timestamp 在本次测试起止时间之间。</p>
     */
    @Test
    void fallsBackForEmptyMethodArgumentValidationErrors() {
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(
                mock(MethodParameter.class), bindingResult());

        ResponseEntity<ErrorResponse> response = handler.handleValidationException(exception, request);

        assertResponse(response, ErrorCode.VALIDATION_FAILED, ErrorCode.VALIDATION_FAILED.getMessage());
    }

    /**
     * 避免绑定错误缺少明细时返回空白消息。
     *
     * <p>测试输入：字段错误列表为空的 BindException。 请求 URI 均为 /api/test。</p>
     * <p>预期结果：返回 HTTP 422、REQ-422 和默认消息“请求参数校验失败”。 所有响应同时验证 body.status 与 HTTP 状态一致、path 为 /api/test、timestamp 在本次测试起止时间之间。</p>
     */
    @Test
    void fallsBackForEmptyBindErrors() {
        BindException exception = new BindException(bindingResult());

        ResponseEntity<ErrorResponse> response = handler.handleValidationException(exception, request);

        assertResponse(response, ErrorCode.VALIDATION_FAILED, ErrorCode.VALIDATION_FAILED.getMessage());
    }

    /**
     * 将约束校验路径与原因组合为调用方可识别的错误信息。
     *
     * <p>测试输入：包含路径 user.email、消息 must be valid 的约束违反异常。 请求 URI 均为 /api/test。</p>
     * <p>预期结果：返回 HTTP 422、REQ-422 和消息 user.email: must be valid。 所有响应同时验证 body.status 与 HTTP 状态一致、path 为 /api/test、timestamp 在本次测试起止时间之间。</p>
     */
    @Test
    void handlesNonEmptyConstraintViolation() {
        @SuppressWarnings("unchecked")
        ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
        Path propertyPath = mock(Path.class);
        when(propertyPath.toString()).thenReturn("user.email");
        when(violation.getPropertyPath()).thenReturn(propertyPath);
        when(violation.getMessage()).thenReturn("must be valid");

        ResponseEntity<ErrorResponse> response = handler.handleConstraintViolation(
                new ConstraintViolationException(Set.of(violation)), request);

        assertResponse(response, ErrorCode.VALIDATION_FAILED, "user.email: must be valid");
    }

    /**
     * 约束异常没有明细时使用统一默认描述，保持响应可读。
     *
     * <p>测试输入：约束违反集合为空的 ConstraintViolationException。 请求 URI 均为 /api/test。</p>
     * <p>预期结果：返回 HTTP 422、REQ-422 和默认消息“请求参数校验失败”。 所有响应同时验证 body.status 与 HTTP 状态一致、path 为 /api/test、timestamp 在本次测试起止时间之间。</p>
     */
    @Test
    void fallsBackForEmptyConstraintViolations() {
        ResponseEntity<ErrorResponse> response = handler.handleConstraintViolation(
                new ConstraintViolationException(Collections.emptySet()), request);

        assertResponse(response, ErrorCode.VALIDATION_FAILED, ErrorCode.VALIDATION_FAILED.getMessage());
    }

    /**
     * 将请求体解析问题转换为客户端错误，并避免泄露底层解析信息。
     *
     * <p>测试输入：消息为 secret parser detail 的 HttpMessageNotReadableException。 请求 URI 均为 /api/test。</p>
     * <p>预期结果：返回 HTTP 400、REQ-400 和“请求体格式错误或不可解析”，不包含原解析器详情。 所有响应同时验证 body.status 与 HTTP 状态一致、path 为 /api/test、timestamp 在本次测试起止时间之间。</p>
     */
    @Test
    void handlesUnreadableMessageAsBadRequestWithoutUsingExceptionDetail() {
        HttpMessageNotReadableException exception = new HttpMessageNotReadableException(
                "secret parser detail", new MockHttpInputMessage(new byte[0]));

        ResponseEntity<ErrorResponse> response = handler.handleHttpMessageNotReadable(exception, request);

        assertResponse(response, ErrorCode.BAD_REQUEST, "请求体格式错误或不可解析");
        assertThat(Objects.requireNonNull(response.getBody()).message()).doesNotContain("secret parser detail");
    }

    /**
     * 向调用方明确报告请求方法不被支持，而非误报服务器内部错误。
     *
     * <p>测试输入：针对 PATCH 创建的 HttpRequestMethodNotSupportedException。 请求 URI 均为 /api/test。</p>
     * <p>预期结果：返回 HTTP 405、REQ-405，消息等于异常自身的消息。 所有响应同时验证 body.status 与 HTTP 状态一致、path 为 /api/test、timestamp 在本次测试起止时间之间。</p>
     */
    @Test
    void handlesUnsupportedMethod() {
        HttpRequestMethodNotSupportedException exception = new HttpRequestMethodNotSupportedException("PATCH");

        ResponseEntity<ErrorResponse> response = handler.handleMethodNotSupported(exception, request);

        assertResponse(response, ErrorCode.METHOD_NOT_ALLOWED, exception.getMessage());
    }

    /**
     * 为不支持的请求体媒体类型提供明确错误分类。
     *
     * <p>测试输入：使用消息 text/plain 构造的 HttpMediaTypeNotSupportedException。 请求 URI 均为 /api/test。</p>
     * <p>预期结果：返回 HTTP 415、REQ-415，消息等于异常自身的消息。 所有响应同时验证 body.status 与 HTTP 状态一致、path 为 /api/test、timestamp 在本次测试起止时间之间。</p>
     */
    @Test
    void handlesUnsupportedMediaType() {
        HttpMediaTypeNotSupportedException exception = new HttpMediaTypeNotSupportedException("text/plain");

        ResponseEntity<ErrorResponse> response = handler.handleMediaTypeNotSupported(exception, request);

        assertResponse(response, ErrorCode.UNSUPPORTED_MEDIA_TYPE, exception.getMessage());
    }

    /**
     * 将查询不到元素的异常映射为资源不存在。
     *
     * <p>测试输入：消息为 item missing 的 NoSuchElementException。 请求 URI 均为 /api/test。</p>
     * <p>预期结果：返回 HTTP 404、REQ-404 和消息 item missing。 所有响应同时验证 body.status 与 HTTP 状态一致、path 为 /api/test、timestamp 在本次测试起止时间之间。</p>
     */
    @Test
    void handlesMissingResource() {
        NoSuchElementException exception = new NoSuchElementException("item missing");

        ResponseEntity<ErrorResponse> response = handler.handleNoSuchElement(exception, request);

        assertResponse(response, ErrorCode.RESOURCE_NOT_FOUND, "item missing");
    }

    /**
     * 将非法业务入参识别为客户端请求错误，并保留具体说明。
     *
     * <p>测试输入：消息为 bad value 的 IllegalArgumentException。 请求 URI 均为 /api/test。</p>
     * <p>预期结果：返回 HTTP 400、REQ-400 和消息 bad value。 所有响应同时验证 body.status 与 HTTP 状态一致、path 为 /api/test、timestamp 在本次测试起止时间之间。</p>
     */
    @Test
    void handlesIllegalArgument() {
        ResponseEntity<ErrorResponse> response = handler.handleIllegalArgument(
                new IllegalArgumentException("bad value"), request);

        assertResponse(response, ErrorCode.BAD_REQUEST, "bad value");
    }

    /**
     * 未知异常统一兜底，避免把内部诊断信息暴露给客户端。
     *
     * <p>测试输入：带有模拟数据库密码文本及内部连接信息 cause 的普通 Exception。 请求 URI 均为 /api/test。</p>
     * <p>预期结果：返回 HTTP 500、SYS-500 和“服务器内部错误”，不包含原消息、cause 详情或异常类名。 所有响应同时验证 body.status 与 HTTP 状态一致、path 为 /api/test、timestamp 在本次测试起止时间之间。</p>
     */
    @Test
    void handlesUnexpectedErrorWithGenericMessageOnly() {
        Exception exception = new Exception("database password=secret",
                new IllegalStateException("internal connection string"));

        ResponseEntity<ErrorResponse> response = handler.handleUnexpected(exception, request);

        assertResponse(response, ErrorCode.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_SERVER_ERROR.getMessage());
        assertThat(Objects.requireNonNull(response.getBody()).message())
                .doesNotContain("database password=secret", "internal connection string", "IllegalStateException");
    }

    private static BindingResult bindingResult(FieldError... errors) {
        MapBindingResult result = new MapBindingResult(new HashMap<>(), "command");
        for (FieldError error : errors) {
            result.addError(error);
        }
        return result;
    }

    private void assertResponse(
            ResponseEntity<ErrorResponse> response, ErrorCode errorCode, String message) {
        assertThat(response.getStatusCode()).isEqualTo(errorCode.getHttpStatus());
        assertThat(response.getBody()).isNotNull();
        ErrorResponse body = Objects.requireNonNull(response.getBody());
        assertThat(body.status()).isEqualTo(errorCode.getHttpStatus().value());
        assertThat(body.code()).isEqualTo(errorCode.getCode());
        assertThat(body.message()).isEqualTo(message);
        assertThat(body.path()).isEqualTo(PATH);
        assertThat(body.timestamp()).isBetween(startedAt, LocalDateTime.now());
    }
}
