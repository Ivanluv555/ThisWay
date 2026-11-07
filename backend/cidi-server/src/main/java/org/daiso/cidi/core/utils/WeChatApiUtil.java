/**
 * WeChatApiUtil
 *
 * 说明：本文件定义了 WeChatApiUtil，是 cidi-server 项目中的核心代码文件之一。
 *
 */

package org.daiso.cidi.core.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.daiso.cidi.core.error.CidiException;
import org.daiso.cidi.core.error.ErrorCode;
import org.daiso.cidi.core.properties.Wxproperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.client.RestTemplate;

@Component
public class WeChatApiUtil {

    private static final Logger log = LoggerFactory.getLogger(WeChatApiUtil.class);

    private final Wxproperties wxproperties;
    private final RestTemplate restTemplate;
    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private String cachedAccessToken;
    private long accessTokenExpireAt;

    private static final String WECHAT_API_URL = "https://api.weixin.qq.com/sns/jscode2session" +
            "?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code";
    private static final String ACCESS_TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/token" +
            "?grant_type=client_credential&appid=%s&secret=%s";

    public record WeChatSessionInfo(String openid, String unionid, String sessionKey) {}

    public WeChatApiUtil(Wxproperties wxproperties, RestTemplate restTemplate, WebClient.Builder webClientBuilder) {
        this.wxproperties = wxproperties;
        this.restTemplate = restTemplate;
        this.webClient = webClientBuilder.build();
    }

    public WeChatSessionInfo getSessionInfoFromCode(String code) {
        if (!StringUtils.hasText(code)) {
            throw new CidiException(ErrorCode.BAD_REQUEST, "微信登录 code 不能为空");
        }

        if (!hasValidConfig()) {
            if (wxproperties.isMockLoginEnabled()) {
                log.warn("WX config missing, using mock login session in current profile");
                return buildMockSessionInfo(code);
            }
            throw new CidiException(ErrorCode.WECHAT_CONFIG_MISSING, "请检查 WX_APPID/WX_SECRET 配置");
        }

        String encodedCode = URLEncoder.encode(code, StandardCharsets.UTF_8);
        String url = String.format(WECHAT_API_URL, wxproperties.getAppid(), wxproperties.getSecret(), encodedCode);

        try {
            String responseJson = restTemplate.getForObject(url, String.class);
            JsonNode rootNode = objectMapper.readTree(responseJson);

            handleWeChatError(rootNode, "code2Session");

            String openid = rootNode.path("openid").asText();
            if (!StringUtils.hasText(openid)) {
                throw new CidiException(ErrorCode.WECHAT_IDENTIFIER_MISSING);
            }

            return new WeChatSessionInfo(
                    openid,
                    rootNode.path("unionid").asText(null),
                    rootNode.path("session_key").asText(null)
            );
        } catch (CidiException ex) {
            if (wxproperties.isMockLoginEnabled() && isRetryableWechatError(ex)) {
                log.warn("WeChat login failed, fallback to mock session. cause={}", ex.getMessage());
                return buildMockSessionInfo(code);
            }
            throw ex;
        } catch (Exception ex) {
            if (wxproperties.isMockLoginEnabled()) {
                log.warn("WeChat login exception, fallback to mock session", ex);
                return buildMockSessionInfo(code);
            }
            throw new CidiException(ErrorCode.WECHAT_API_ERROR, "调用微信接口异常", ex);
        }
    }

    public String getPhoneNumberByCode(String phoneCode) {
        validateConfig();
        if (!StringUtils.hasText(phoneCode)) {
            throw new CidiException(ErrorCode.BAD_REQUEST, "手机号 code 不能为空");
        }
        String accessToken = getAccessToken();
        String apiUrl = wxproperties.getPhoneApiUrl();
        if (!StringUtils.hasText(apiUrl)) {
            throw new CidiException(ErrorCode.WECHAT_CONFIG_MISSING, "微信手机号接口地址未配置");
        }

        try {
            String responseJson = webClient.post()
                    .uri(apiUrl + "?access_token=" + URLEncoder.encode(accessToken, StandardCharsets.UTF_8))
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("{\"code\":\"" + escapeJson(phoneCode) + "\"}")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode rootNode = objectMapper.readTree(responseJson);
            handleWeChatError(rootNode, "getPhoneNumber");

            JsonNode phoneInfo = rootNode.path("phone_info");
            String phoneNumber = phoneInfo.path("purePhoneNumber").asText();
            if (!StringUtils.hasText(phoneNumber)) {
                phoneNumber = phoneInfo.path("phoneNumber").asText();
            }
            if (!StringUtils.hasText(phoneNumber)) {
                throw new CidiException(ErrorCode.WECHAT_PHONE_NUMBER_MISSING);
            }
            return phoneNumber;
        } catch (CidiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new CidiException(ErrorCode.WECHAT_API_ERROR, "调用微信手机号接口异常", ex);
        }
    }

    private synchronized String getAccessToken() {
        long now = System.currentTimeMillis();
        if (StringUtils.hasText(cachedAccessToken) && now < accessTokenExpireAt) {
            return cachedAccessToken;
        }

        String url = String.format(ACCESS_TOKEN_URL, wxproperties.getAppid(), wxproperties.getSecret());
        try {
            String responseJson = restTemplate.getForObject(url, String.class);
            JsonNode rootNode = objectMapper.readTree(responseJson);
            handleWeChatError(rootNode, "accessToken");

            String token = rootNode.path("access_token").asText();
            int expiresIn = rootNode.path("expires_in").asInt(7200);
            if (!StringUtils.hasText(token)) {
                throw new CidiException(ErrorCode.WECHAT_API_ERROR, "微信 access_token 缺失");
            }

            this.cachedAccessToken = token;
            this.accessTokenExpireAt = now + Math.max(expiresIn - 300, 60) * 1000L;
            return token;
        } catch (CidiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new CidiException(ErrorCode.WECHAT_API_ERROR, "获取微信 access_token 失败", ex);
        }
    }

    private void validateConfig() {
        if (!StringUtils.hasText(wxproperties.getAppid()) || !StringUtils.hasText(wxproperties.getSecret())) {
            throw new CidiException(ErrorCode.WECHAT_CONFIG_MISSING, "请检查 WX_APPID/WX_SECRET 配置");
        }
    }

    private boolean hasValidConfig() {
        return StringUtils.hasText(wxproperties.getAppid()) && StringUtils.hasText(wxproperties.getSecret());
    }

    private boolean isRetryableWechatError(CidiException ex) {
        ErrorCode code = ex.getErrorCode();
        return code == ErrorCode.WECHAT_API_ERROR || code == ErrorCode.WECHAT_CONFIG_MISSING;
    }

    private WeChatSessionInfo buildMockSessionInfo(String code) {
        String normalized = code.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        if (!StringUtils.hasText(normalized)) {
            normalized = "anonymous";
        }
        if (normalized.length() > 20) {
            normalized = normalized.substring(0, 20);
        }
        String openid = "mock_" + normalized;
        String sessionKey = "mock_session_" + normalized;
        return new WeChatSessionInfo(openid, null, sessionKey);
    }

    private void handleWeChatError(JsonNode rootNode, String operation) {
        int errCode = rootNode.path("errcode").asInt(0);
        if (errCode == 0) {
            return;
        }
        String errMsg = rootNode.path("errmsg").asText("unknown");
        if (errCode == 40029 || errCode == 40163) {
            throw new CidiException(ErrorCode.WECHAT_INVALID_CODE, "微信 " + operation + " 失败: " + errMsg);
        }
        throw new CidiException(ErrorCode.WECHAT_API_ERROR, "微信 " + operation + " 失败: " + errMsg);
    }

    private String escapeJson(String raw) {
        if (raw == null) {
            throw new CidiException(ErrorCode.BAD_REQUEST, "请求参数不能为空");
        }
        return raw.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}