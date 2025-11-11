package org.daiso.cidi.core.utils;

import org.springframework.stereotype.Component;

@Component
public class WeChatApiUtil {

    public String getUnionIdFromCode(String code) {
        // 这里是模拟从微信服务器获取 UnionID 的逻辑
        return "mocked_union_id";
    }

    public String getAccessToken() {
        // 这里是模拟从微信服务器获取 AccessToken 的逻辑
        return "mocked_access_token";
    }

    public String getUserInfo(String accessToken, String openId) {
        // 这里是模拟从微信服务器获取用户信息的逻辑
        return "mocked_user_info";
    }
}
