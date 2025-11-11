package org.daiso.cidi.core.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "wx")
@Component
class Wxproperties {

    @Setter
    @Getter
    private String appid;
    @Setter
    @Getter
    private String secret;
}
