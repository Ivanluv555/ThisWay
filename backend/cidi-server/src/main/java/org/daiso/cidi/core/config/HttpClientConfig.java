/**
 * HttpClientConfig
 *
 * 说明：本文件定义了 HttpClientConfig
 * 是全项目进行HTTP请求的配置类，提供了一个 RestTemplate Bean 供全局使用。
 * 通过使用 RestTemplateBuilder 来构建 RestTemplate，可以方便地进行定制化
 * 这样做可以
 */

package org.daiso.cidi.core.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class HttpClientConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }
}
