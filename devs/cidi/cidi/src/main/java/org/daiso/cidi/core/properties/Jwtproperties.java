package org.daiso.cidi.core.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "jwt")
@Component
class Jwtproperties {
    @Getter
    @Setter
    private String secret;

    @Setter
    @Getter
    private long expiration;
}
