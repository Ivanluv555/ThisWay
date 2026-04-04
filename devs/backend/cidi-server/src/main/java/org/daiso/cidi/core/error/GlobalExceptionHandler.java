/**
 * GlobalExceptionHandler
 *
 * 说明：本文件定义了 GlobalExceptionHandler，是 cidi-server 项目中的核心代码文件之一。
 *
 */

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
