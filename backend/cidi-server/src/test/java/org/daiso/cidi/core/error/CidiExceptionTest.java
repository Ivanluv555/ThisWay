package org.daiso.cidi.core.error;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class CidiExceptionTest {

    /**
     * 保证所有业务错误都可以直接使用统一的默认错误描述。
     *
     * <p>测试输入：逐一传入 ErrorCode 的全部 14 个枚举值，调用仅接收错误码的构造器。</p>
     * <p>预期结果：每组均得到 RuntimeException 子类；错误码保持原实例，消息等于该枚举默认消息，cause 为空。</p>
     *
     * @param errorCode 当前测试的业务错误枚举，由全部 ErrorCode 枚举值逐一提供
     */
    @ParameterizedTest
    @EnumSource(ErrorCode.class)
    void constructorUsesDefaultMessage(ErrorCode errorCode) {
        CidiException exception = new CidiException(errorCode);

        assertThat(exception).isInstanceOf(RuntimeException.class)
                .hasMessage(errorCode.getMessage())
                .hasNoCause();
        assertThat(exception.getErrorCode()).isSameAs(errorCode);
    }

    /**
     * 允许业务补充具体失败原因，同时保留统一错误分类。
     *
     * <p>测试输入：逐一传入全部 14 个 ErrorCode，并指定消息 specific failure。</p>
     * <p>预期结果：每组均保留原错误码和自定义消息，cause 为空。</p>
     *
     * @param errorCode 当前测试的业务错误枚举，由全部 ErrorCode 枚举值逐一提供
     */
    @ParameterizedTest
    @EnumSource(ErrorCode.class)
    void constructorPreservesDetailMessage(ErrorCode errorCode) {
        CidiException exception = new CidiException(errorCode, "specific failure");

        assertThat(exception).hasMessage("specific failure").hasNoCause();
        assertThat(exception.getErrorCode()).isSameAs(errorCode);
    }

    /**
     * 包装底层异常时保留异常链，便于定位原因而不丢失业务错误分类。
     *
     * <p>测试输入：逐一传入全部 14 个 ErrorCode、自定义消息 specific failure 和 IllegalStateException 原因对象。</p>
     * <p>预期结果：每组均保留原错误码、自定义消息及完全相同的 cause 对象。</p>
     *
     * @param errorCode 当前测试的业务错误枚举，由全部 ErrorCode 枚举值逐一提供
     */
    @ParameterizedTest
    @EnumSource(ErrorCode.class)
    void constructorPreservesDetailMessageAndCause(ErrorCode errorCode) {
        Throwable cause = new IllegalStateException("underlying failure");

        CidiException exception = new CidiException(errorCode, "specific failure", cause);

        assertThat(exception).hasMessage("specific failure").hasCause(cause);
        assertThat(exception.getCause()).isSameAs(cause);
        assertThat(exception.getErrorCode()).isSameAs(errorCode);
    }

    /**
     * 记录显式传入空消息时不自动回退到默认消息的现有构造器契约。
     *
     * <p>测试输入：BAD_REQUEST 与显式 null 消息，分别调用双参数构造器和 cause 为 null 的三参数构造器。</p>
     * <p>预期结果：两种构造方式的 message 和 cause 均为 null，错误码均保持 BAD_REQUEST。</p>
     */
    @Test
    void constructorsDoNotSubstituteDefaultMessageForExplicitNull() {
        CidiException withoutCause = new CidiException(ErrorCode.BAD_REQUEST, null);
        CidiException withCause = new CidiException(ErrorCode.BAD_REQUEST, null, null);

        assertThat(withoutCause.getMessage()).isNull();
        assertThat(withCause.getMessage()).isNull();
        assertThat(withoutCause).hasNoCause();
        assertThat(withCause).hasNoCause();
        assertThat(withoutCause.getErrorCode()).isSameAs(ErrorCode.BAD_REQUEST);
        assertThat(withCause.getErrorCode()).isSameAs(ErrorCode.BAD_REQUEST);
    }
}
