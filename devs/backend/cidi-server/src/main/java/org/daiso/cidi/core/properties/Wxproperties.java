package org.daiso.cidi.core.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "wx")
@Component
public class Wxproperties {

    private String appid;
    private String secret;
    private String miniProgramEnv;
    private String phoneApiUrl;

    public String getAppid() {
        return appid;
    }

    public void setAppid(String appid) {
        this.appid = appid;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getMiniProgramEnv() {
        return miniProgramEnv;
    }

    public void setMiniProgramEnv(String miniProgramEnv) {
        this.miniProgramEnv = miniProgramEnv;
    }

    public String getPhoneApiUrl() {
        return phoneApiUrl;
    }

    public void setPhoneApiUrl(String phoneApiUrl) {
        this.phoneApiUrl = phoneApiUrl;
    }
}
