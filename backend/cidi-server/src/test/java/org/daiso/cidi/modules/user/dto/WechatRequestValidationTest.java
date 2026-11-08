package org.daiso.cidi.modules.user.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class WechatRequestValidationTest {
    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        factory.close();
    }

    /**
     * 验证登录 DTO 的 code 约束：输入空引用、空串或空白，预期仅 code 字段报告明确的中文错误。
     *
     * @param code 不合法的登录凭证
     */
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n"})
    void loginCodeMustContainText(String code) {
        var violations = validator.validate(new WechatLoginRequest(code, null, null));
        assertThat(violations).singleElement().satisfies(violation -> {
            assertThat(violation.getPropertyPath().toString()).isEqualTo("code");
            assertThat(violation.getMessage()).isEqualTo("code 不能为空");
        });
    }

    /**
     * 验证登录 DTO 不强制填写个人资料：输入有效 code、空昵称及空头像，预期没有约束违反。
     */
    @Test
    void loginAcceptsCodeWithoutProfile() {
        assertThat(validator.validate(new WechatLoginRequest("code", null, null))).isEmpty();
    }

    /**
     * 验证绑定 DTO 的 code 约束：输入合法用户标识及空白凭证，预期仅 code 字段校验失败。
     *
     * @param code 不合法的手机号授权凭证
     */
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n"})
    void bindingCodeMustContainText(String code) {
        var violations = validator.validate(new WechatBindPhoneRequest(1L, code));
        assertThat(violations).singleElement().satisfies(violation -> {
            assertThat(violation.getPropertyPath().toString()).isEqualTo("code");
            assertThat(violation.getMessage()).isEqualTo("code 不能为空");
        });
    }

    /**
     * 验证绑定必须指定用户：输入空用户标识和合法凭证，预期仅 userId 字段报告必填错误。
     */
    @Test
    void bindingUserIdIsRequired() {
        var violations = validator.validate(new WechatBindPhoneRequest(null, "code"));
        assertThat(violations).singleElement().satisfies(violation -> {
            assertThat(violation.getPropertyPath().toString()).isEqualTo("userId");
            assertThat(violation.getMessage()).isEqualTo("userId 不能为空");
        });
    }

    /**
     * 验证绑定 DTO 汇总独立字段错误：输入两个空值，预期同时报告 userId 与 code，而非遗漏任一约束。
     */
    @Test
    void bindingReportsBothMissingFields() {
        assertThat(validator.validate(new WechatBindPhoneRequest(null, null)))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactlyInAnyOrder("userId", "code");
    }

    /**
     * 验证合法绑定数据可通过校验：输入正数用户标识和有效凭证，预期无约束违反。
     */
    @Test
    void bindingAcceptsValidRequest() {
        assertThat(validator.validate(new WechatBindPhoneRequest(1L, "phone-code"))).isEmpty();
    }
}
