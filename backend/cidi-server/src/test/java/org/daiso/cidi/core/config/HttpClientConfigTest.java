package org.daiso.cidi.core.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class HttpClientConfigTest {
    /**
     * 确认统一客户端配置保留调用方的 Builder 定制；本用例只展开 URI，不发送网络请求。
     *
     * <p>测试输入：带有基础地址 https://example.test 定制的 RestTemplateBuilder，以及相对路径 /users。</p>
     * <p>预期结果：构建出的客户端将相对路径展开为 https://example.test/users。</p>
     */
    @Test
    void preservesBuilderCustomization() {
        RestTemplate template = new HttpClientConfig().restTemplate(
                new RestTemplateBuilder().additionalCustomizers(client -> client.setUriTemplateHandler(
                        new org.springframework.web.util.DefaultUriBuilderFactory("https://example.test"))));
        assertThat(template.getUriTemplateHandler().expand("/users").toString())
                .isEqualTo("https://example.test/users");
    }

    /**
     * 确认业务类可以注入并复用统一 HTTP 客户端，无需启动完整应用或连接数据库。
     *
     * <p>测试输入：仅注册 RestTemplateBuilder 和 HttpClientConfig 的轻量 Spring 上下文。</p>
     * <p>预期结果：上下文启动成功，只有一个 RestTemplate Bean；按名称 restTemplate 和按类型获取的是同一实例。</p>
     */
    @Test
    void registersSingleReusableRestTemplateBean() {
        new ApplicationContextRunner()
                .withBean(RestTemplateBuilder.class, RestTemplateBuilder::new)
                .withUserConfiguration(HttpClientConfig.class)
                .run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(RestTemplate.class);
                    assertThat(context.getBean("restTemplate"))
                            .isSameAs(context.getBean(RestTemplate.class));
                });
    }
}
