package org.daiso.cidi.core.properties;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class CorePropertiesTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(PropertiesConfiguration.class);

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties({Jwtproperties.class, Wxproperties.class})
    static class PropertiesConfiguration {}

    /**
     * 确认 JWT 密钥与毫秒单位的有效期能够从外部配置绑定到业务配置对象。
     *
     * <p>测试输入：jwt.secret=test-secret，jwt.expiration=3600000。</p>
     * <p>预期结果：Jwtproperties 中的 secret 为 test-secret，expiration 为 3600000，绑定上下文启动成功。</p>
     */
    @Test
    void bindsJwtProperties() {
        runner.withPropertyValues("jwt.secret=test-secret", "jwt.expiration=3600000")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    Jwtproperties properties = context.getBean(Jwtproperties.class);
                    assertThat(properties.getSecret()).isEqualTo("test-secret");
                    assertThat(properties.getExpiration()).isEqualTo(3_600_000L);
                });
    }

    /**
     * 确认连字符形式的配置名称可绑定到 Java 驼峰属性，微信调用及模拟登录开关不会漏配。
     *
     * <p>测试输入：wx.appid=test-app、secret=test-secret、mini-program-env=trial、phone-api-url=https://example.test/phone、mock-login-enabled=true。</p>
     * <p>预期结果：Wxproperties 的五个对应属性均等于输入值，绑定上下文启动成功。</p>
     */
    @Test
    void bindsAllWechatPropertiesUsingRelaxedNames() {
        runner.withPropertyValues("wx.appid=test-app", "wx.secret=test-secret",
                        "wx.mini-program-env=trial", "wx.phone-api-url=https://example.test/phone",
                        "wx.mock-login-enabled=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    Wxproperties properties = context.getBean(Wxproperties.class);
                    assertThat(properties.getAppid()).isEqualTo("test-app");
                    assertThat(properties.getSecret()).isEqualTo("test-secret");
                    assertThat(properties.getMiniProgramEnv()).isEqualTo("trial");
                    assertThat(properties.getPhoneApiUrl()).isEqualTo("https://example.test/phone");
                    assertThat(properties.isMockLoginEnabled()).isTrue();
                });
    }

    /**
     * 记录配置对象自身的默认值，确认模拟登录不会默认开启；本测试不表示业务调用在缺配置时可成功。
     *
     * <p>测试输入：未提供 jwt 与 wx 配置的轻量属性绑定上下文。</p>
     * <p>预期结果：所有字符串属性为 null，JWT 有效期为 0，mockLoginEnabled 为 false，上下文仍能启动。</p>
     */
    @Test
    void missingPropertiesKeepDefaultsAndMockLoginDisabled() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            Jwtproperties jwt = context.getBean(Jwtproperties.class);
            assertThat(jwt.getSecret()).isNull();
            assertThat(jwt.getExpiration()).isZero();
            Wxproperties wx = context.getBean(Wxproperties.class);
            assertThat(wx.getAppid()).isNull();
            assertThat(wx.getSecret()).isNull();
            assertThat(wx.getMiniProgramEnv()).isNull();
            assertThat(wx.getPhoneApiUrl()).isNull();
            assertThat(wx.isMockLoginEnabled()).isFalse();
        });
    }

    /**
     * 防止无法转换为 long 的有效期配置被静默接受。
     *
     * <p>测试输入：jwt.expiration=not-a-number。</p>
     * <p>预期结果：属性绑定失败，轻量上下文启动失败。</p>
     */
    @Test
    void rejectsNonNumericJwtExpiration() {
        runner.withPropertyValues("jwt.expiration=not-a-number")
                .run(context -> assertThat(context).hasFailed());
    }
}
