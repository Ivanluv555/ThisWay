package org.daiso.cidi.core.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * REST 控制器的统一异常转换器，将业务异常、参数错误和未预期异常转换为 {@link ErrorResponse}。
 *
 * <p>{@link CidiException} 保留其业务错误码与消息；Bean Validation 失败返回 HTTP 422，参数或请求体错误返回 HTTP 400。
 *
 * <p>同时处理资源缺失、请求方法及媒体类型错误；未预期异常返回通用 HTTP 500 提示，响应不暴露其原始消息。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CidiException.class)
    public org.springframework.http.ResponseEntity<ErrorResponse> handleCidiException(
            CidiException ex,
            HttpServletRequest request
    ) {
        return buildResponse(ex.getErrorCode(), ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public org.springframework.http.ResponseEntity<ErrorResponse> handleValidationException(
            Exception ex,
            HttpServletRequest request
    ) {
        String detail;
        if (ex instanceof MethodArgumentNotValidException manve) {
            detail = manve.getBindingResult().getFieldErrors().stream()
                    .map(err -> err.getField() + ": " + err.getDefaultMessage())
                    .collect(Collectors.joining("; "));
        } else {
            BindException be = (BindException) ex;
            detail = be.getBindingResult().getFieldErrors().stream()
                    .map(err -> err.getField() + ": " + err.getDefaultMessage())
                    .collect(Collectors.joining("; "));
        }

        if (detail.isBlank()) {
            detail = ErrorCode.VALIDATION_FAILED.getMessage();
        }

        return buildResponse(ErrorCode.VALIDATION_FAILED, detail, request.getRequestURI());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public org.springframework.http.ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request
    ) {
        String detail = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining("; "));

        if (detail.isBlank()) {
            detail = ErrorCode.VALIDATION_FAILED.getMessage();
        }

        return buildResponse(ErrorCode.VALIDATION_FAILED, detail, request.getRequestURI());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public org.springframework.http.ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        return buildResponse(ErrorCode.BAD_REQUEST, "请求体格式错误或不可解析", request.getRequestURI());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public org.springframework.http.ResponseEntity<ErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request
    ) {
        return buildResponse(ErrorCode.METHOD_NOT_ALLOWED, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public org.springframework.http.ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException ex,
            HttpServletRequest request
    ) {
        return buildResponse(ErrorCode.UNSUPPORTED_MEDIA_TYPE, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(NoSuchElementException.class)
    public org.springframework.http.ResponseEntity<ErrorResponse> handleNoSuchElement(
            NoSuchElementException ex,
            HttpServletRequest request
    ) {
        return buildResponse(ErrorCode.RESOURCE_NOT_FOUND, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public org.springframework.http.ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request
    ) {
        return buildResponse(ErrorCode.BAD_REQUEST, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    public org.springframework.http.ResponseEntity<ErrorResponse> handleUnexpected(
            Exception ex,
            HttpServletRequest request
    ) {
        return buildResponse(ErrorCode.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_SERVER_ERROR.getMessage(), request.getRequestURI());
    }

    private org.springframework.http.ResponseEntity<ErrorResponse> buildResponse(
            ErrorCode errorCode,
            String message,
            String path
    ) {
        HttpStatus status = errorCode.getHttpStatus();
        ErrorResponse response = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                errorCode.getCode(),
                message,
                path
        );

        return new org.springframework.http.ResponseEntity<>(response, status);
    }
}
