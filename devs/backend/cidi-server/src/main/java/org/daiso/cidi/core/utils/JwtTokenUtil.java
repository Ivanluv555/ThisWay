/**
 * JwtTokenUtil
 *
 * 说明：本文件定义了 JwtTokenUtil
 * JwtTokenUtil是一个可以鉴别用户身份的工具
 * 提供生成、解析和验证 JWT Token 的相关功能
 */

package org.daiso.cidi.core.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.daiso.cidi.core.properties.Jwtproperties;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

@Component // 注册为 Spring 组件
public class JwtTokenUtil {

    private final Jwtproperties jwtproperties;

    public JwtTokenUtil(Jwtproperties jwtproperties) {
        this.jwtproperties = jwtproperties;
    }

    // 1. 根据 secret 生成一个安全的密钥
    private SecretKey getKey() {
        String secret = jwtproperties.getSecret(); // 从配置中获取 secret
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    /**
     * 生成Token
     * @param userId 用户的 userID，我们将把它作为 "subject"
     * @return JWT Token 字符串
     */
    public String generateToken(String userId) {
        Date now = new Date();
        long expiration = jwtproperties.getExpiration(); // 过期时间，单位毫秒
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .setSubject(userId) // 核心信息：存入用户ID
                .setIssuedAt(now)   // 签发时间
                .setExpiration(expiryDate) // 过期时间
                .signWith(getKey(), SignatureAlgorithm.HS512) // 签名
                .compact();
    }

    // expose configured expiration in milliseconds
    public Long getExpirationMillis() {
        return jwtproperties.getExpiration();
    }

    // (后续需要) 从 Token 中解析出用户ID
    public String getUserIdFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    // (后续需要) 验证 Token 是否过期
    public boolean isTokenExpired(String token) {
        Date expiration = getClaimFromToken(token, Claims::getExpiration);
        return expiration.before(new Date());
    }

    // --- 内部辅助方法 ---
    private <T> T getClaimFromToken(String token, @NonNull Function<Claims, T> claimsResolver) {
        Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}