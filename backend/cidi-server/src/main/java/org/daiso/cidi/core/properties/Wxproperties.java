package org.daiso.cidi.core.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 微信小程序接入参数，绑定 {@code wx} 前缀的 Spring 配置。
 *
 * <p>{@code appid} 和 {@code secret} 用于微信会话及 access_token 请求，{@code phoneApiUrl} 指定手机号授权接口。
 *
 * <p>{@code miniProgramEnv} 作为环境标识返回给登录调用方；{@code mockLoginEnabled} 控制登录异常时的模拟会话回退，不作用于手机号绑定。
 */
@ConfigurationProperties(prefix = "wx")
@Component
public class Wxproperties {

    /** 微信小程序应用标识。 */
    private String appid;
    /** 微信小程序服务端调用密钥。 */
    private String secret;
    /** 登录响应透传的环境标识，默认配置为 dev。 */
    private String miniProgramEnv;
    /** 微信手机号授权兑换接口地址。 */
    private String phoneApiUrl;
    /** 是否允许登录在配置缺失或部分调用失败时回退为模拟会话，默认关闭。 */
    private boolean mockLoginEnabled;

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

    public boolean isMockLoginEnabled() {
        return mockLoginEnabled;
    }

    public void setMockLoginEnabled(boolean mockLoginEnabled) {
        this.mockLoginEnabled = mockLoginEnabled;
    }
}
