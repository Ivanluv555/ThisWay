package org.daiso.cidi.core.utils;

import org.daiso.cidi.core.error.CidiException;
import org.daiso.cidi.core.error.ErrorCode;
import org.daiso.cidi.core.properties.Wxproperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component // 注册为 Spring 组件，这样 Service 才能“注入” 它
public class WeChatApiUtil {

    private final Wxproperties wxproperties;

    // 2. 准备一个 HTTP 请求工具
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 微信 code2Session API 地址
    private static final String WECHAT_API_URL = "https://api.weixin.qq.com/sns/jscode2session" +
            "?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code";

    public WeChatApiUtil(Wxproperties wxproperties) {
        this.wxproperties = wxproperties;
    }

    /**
     * 这就是您在 UserService 中调用的方法
     */
    public String getUnionIdFromCode(String code) {
        // 3. 拼接请求 URL
        String url = String.format(WECHAT_API_URL, wxproperties.getAppid(), wxproperties.getSecret(), code);

        try {
            // 4. 从您的后端服务器“发起”对微信服务器的 GET 请求
            String responseJson = restTemplate.getForObject(url, String.class);

            // 5. 解析微信返回的 JSON 数据
            JsonNode rootNode = objectMapper.readTree(responseJson);

            if (rootNode.has("errcode") && rootNode.get("errcode").asInt() != 0) {
                // 如果微信返回错误
                throw new CidiException(
                        ErrorCode.WECHAT_API_ERROR,
                        "微信 code2Session 失败: " + rootNode.path("errmsg").asText("unknown")
                );
            }

            // 6. 成功，返回 UnionID
            // 注意：您需要确保您的小程序已绑定到“微信开放平台”才能获取 UnionID，
            // 否则这里应该获取 "openid"
            if (rootNode.has("unionid")) {
                return rootNode.get("unionid").asText();
            }
            // 如果没有 unionid，退而求其次使用 openid
            if (rootNode.has("openid")) {
                return rootNode.get("openid").asText(); // 您需要修改数据库字段来存 openid
            }

            throw new CidiException(ErrorCode.WECHAT_IDENTIFIER_MISSING);
        } catch (CidiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new CidiException(ErrorCode.WECHAT_API_ERROR, "调用微信接口异常", ex);
        }
    }
}