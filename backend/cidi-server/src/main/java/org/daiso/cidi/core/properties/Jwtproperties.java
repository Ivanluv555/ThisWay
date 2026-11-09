package org.daiso.cidi.core.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 签发与解析参数，绑定 {@code jwt} 前缀的 Spring 配置。
 *
 * <p>{@code secret} 用于构建 HMAC 密钥，{@code expiration} 表示令牌有效时长，单位为毫秒。
 *
 * <p>由 {@code JwtTokenUtil} 使用 HS512 签名；配置需提供该算法所需的足够长度的密钥，本类只负责属性绑定。
 */
@ConfigurationProperties(prefix = "jwt")
@Component
public class Jwtproperties {
    /** JWT 签名密钥原文，由令牌工具转换为 HMAC 密钥。 */
    private String secret;
    /** 从签发时刻起计算的有效时长，单位为毫秒。 */
    private long expiration;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getExpiration() {
        return expiration;
    }

    public void setExpiration(long expiration) {
        this.expiration = expiration;
    }
}
