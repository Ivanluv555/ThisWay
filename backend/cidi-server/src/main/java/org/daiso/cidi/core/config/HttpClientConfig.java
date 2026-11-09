package org.daiso.cidi.core.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * 同步 HTTP 客户端的 Spring 配置，提供共享的 {@link RestTemplate} Bean。
 *
 * <p>通过 Spring Boot 提供的 {@link RestTemplateBuilder} 构建客户端，供微信会话和 access_token 请求使用。
 *
 * <p>本类沿用构建器的配置，未额外设置超时、重试或拦截器；微信手机号请求使用独立的 WebClient。
 */
@Configuration
public class HttpClientConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }
}
