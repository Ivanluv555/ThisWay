package org.daiso.cidi.core.utils;

import org.springframework.stereotype.Component;

@Component
public class JwtTokenUtil {

    public String generateToken(String userId) {
        // 这里可以生成 JWT Token，并返回给小程序
        return "JWT Token for user " + userId;
    }

    public String validateToken(String token) {
        // 这里可以验证 JWT Token，并返回用户 ID
        return "User ID from token: " + token;
    }
}
