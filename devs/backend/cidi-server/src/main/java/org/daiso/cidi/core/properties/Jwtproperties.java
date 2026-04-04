/**
 * Jwtproperties
 *
 * 说明：本文件定义了 Jwtproperties，是 cidi-server 项目中的核心代码文件之一。
 *
 */

package org.daiso.cidi.core.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "jwt")
@Component
public class Jwtproperties {
    private String secret;
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
