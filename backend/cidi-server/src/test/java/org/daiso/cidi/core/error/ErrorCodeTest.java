package org.daiso.cidi.core.error;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorCodeTest {

    /**
     * 维护对外错误响应契约，特别区分业务错误码后缀与实际 HTTP 状态，避免客户端误判。
     *
     * <p>测试输入：expectedMappings() 提供的全部 14 组错误枚举、错误码字符串、中文消息和 HTTP 状态。</p>
     * <p>预期结果：每组枚举的三个 getter 与该行预期值一致；SYS-500 对应 500，REQ 各码对应相应 HTTP 状态，WX-502/503/506 对应 502，WX-504 对应 500，WX-505 对应 400，USR-409 对应 409。</p>
     *
     * @param errorCode 待验证的错误枚举
     * @param code 该枚举预期的对外错误码字符串
     * @param message 该枚举预期的中文错误消息
     * @param status 该枚举预期映射的 HTTP 状态
     */
    @ParameterizedTest(name = "{0} maps to {1} and {3}")
    @MethodSource("expectedMappings")
    void exposesExpectedCodeMessageAndHttpStatus(
            ErrorCode errorCode, String code, String message, HttpStatus status) {
        assertThat(errorCode.getCode()).isEqualTo(code);
        assertThat(errorCode.getMessage()).isEqualTo(message);
        assertThat(errorCode.getHttpStatus()).isSameAs(status);
    }

    /**
     * 防止新增错误码遗漏映射测试，或不同业务错误使用相同编码。
     *
     * <p>测试输入：expectedMappings() 的枚举集合与 ErrorCode.values() 全量枚举。</p>
     * <p>预期结果：两组枚举完全一致且不依赖顺序；所有对外错误码字符串无重复。</p>
     */
    @Test
    void mappingsCoverEveryErrorCodeAndCodesAreUnique() {
        ErrorCode[] covered = expectedMappings()
                .map(arguments -> (ErrorCode) arguments.get()[0])
                .toArray(ErrorCode[]::new);

        assertThat(covered).containsExactlyInAnyOrder(ErrorCode.values());
        assertThat(ErrorCode.values()).extracting(ErrorCode::getCode).doesNotHaveDuplicates();
    }

    private static Stream<Arguments> expectedMappings() {
        return Stream.of(
                Arguments.of(ErrorCode.INTERNAL_SERVER_ERROR, "SYS-500", "服务器内部错误", HttpStatus.INTERNAL_SERVER_ERROR),
                Arguments.of(ErrorCode.BAD_REQUEST, "REQ-400", "请求参数错误", HttpStatus.BAD_REQUEST),
                Arguments.of(ErrorCode.VALIDATION_FAILED, "REQ-422", "请求参数校验失败", HttpStatus.UNPROCESSABLE_ENTITY),
                Arguments.of(ErrorCode.UNAUTHORIZED, "REQ-401", "未授权访问", HttpStatus.UNAUTHORIZED),
                Arguments.of(ErrorCode.FORBIDDEN, "REQ-403", "没有权限访问该资源", HttpStatus.FORBIDDEN),
                Arguments.of(ErrorCode.RESOURCE_NOT_FOUND, "REQ-404", "资源不存在", HttpStatus.NOT_FOUND),
                Arguments.of(ErrorCode.METHOD_NOT_ALLOWED, "REQ-405", "请求方法不被允许", HttpStatus.METHOD_NOT_ALLOWED),
                Arguments.of(ErrorCode.UNSUPPORTED_MEDIA_TYPE, "REQ-415", "不支持的请求体类型", HttpStatus.UNSUPPORTED_MEDIA_TYPE),
                Arguments.of(ErrorCode.WECHAT_API_ERROR, "WX-502", "微信接口调用失败", HttpStatus.BAD_GATEWAY),
                Arguments.of(ErrorCode.WECHAT_IDENTIFIER_MISSING, "WX-503", "微信接口未返回可用标识", HttpStatus.BAD_GATEWAY),
                Arguments.of(ErrorCode.WECHAT_CONFIG_MISSING, "WX-504", "微信配置缺失", HttpStatus.INTERNAL_SERVER_ERROR),
                Arguments.of(ErrorCode.WECHAT_INVALID_CODE, "WX-505", "微信登录凭证无效", HttpStatus.BAD_REQUEST),
                Arguments.of(ErrorCode.WECHAT_PHONE_NUMBER_MISSING, "WX-506", "微信接口未返回手机号", HttpStatus.BAD_GATEWAY),
                Arguments.of(ErrorCode.PHONE_ALREADY_BOUND, "USR-409", "手机号已绑定其他用户", HttpStatus.CONFLICT)
        );
    }
}
