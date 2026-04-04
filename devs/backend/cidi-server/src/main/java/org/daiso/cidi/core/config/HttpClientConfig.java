/**
 * HttpClientConfig
 *
 * 说明：本文件定义了 HttpClientConfig，是 cidi-server 项目中的核心代码文件之一。
 *
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
