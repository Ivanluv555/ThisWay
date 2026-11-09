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

/**
 * 微信小程序服务端 API 适配器，封装登录 code 换取会话及手机号授权 code 换取手机号。
 *
 * <p>会话和 access_token 使用 {@link RestTemplate}；手机号请求通过 WebClient 发出并阻塞等待结果。
 *
 * <p>access_token 在当前实例内同步缓存，刷新期限按微信有效期减 300 秒、至少缓存 60 秒计算。仅登录流程在开启模拟开关且满足回退条件时返回模拟会话；微信错误统一转换为 {@link CidiException}。
 */
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

    /**
     * 登录 code 兑换得到的微信会话，仅在服务端认证流程内使用。
     *
     * <p>本地用户以 openid 关联；unionid 与 sessionKey 当前未持久化，也未包含在登录响应中。
     *
     * @param openid 当前小程序内的微信用户标识；模拟会话使用 mock_ 前缀
     * @param unionid 微信开放平台用户标识，可能为空
     * @param sessionKey 微信会话密钥，真实响应可能为空，模拟会话使用生成的占位值
     */
    public record WeChatSessionInfo(String openid, String unionid, String sessionKey) {}

    public WeChatApiUtil(Wxproperties wxproperties, RestTemplate restTemplate, WebClient.Builder webClientBuilder) {
        this.wxproperties = wxproperties;
        this.restTemplate = restTemplate;
        this.webClient = webClientBuilder.build();
    }

    /**
     * 使用登录 code 调用 code2Session，并确保返回可用的 openid。
     *
     * <p>开启模拟开关时，配置缺失、一般微信 API 错误或调用异常可回退为模拟会话；
     * 已识别的无效 code 和缺失 openid 错误仍抛出。模拟标识由规范化后的 code 生成，不具备真实微信身份语义。
     *
     * @param code 小程序登录凭证，不能为空白
     * @return 真实微信会话或符合回退条件的模拟会话
     * @throws CidiException 参数错误、配置不可用或微信响应失败且未回退
     */
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

    /**
     * 使用手机号授权 code 兑换手机号，优先读取 purePhoneNumber，缺失时读取 phoneNumber。
     *
     * <p>该流程始终要求真实微信配置，并使用缓存的 access_token 调用配置中的手机号接口；不提供模拟回退。
     *
     * @param phoneCode 小程序手机号授权凭证，不能为空白
     * @return 微信返回的非空白手机号
     * @throws CidiException 配置缺失、参数错误、微信调用失败或响应缺少手机号
     */
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
