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

/**
 * 小程序登录令牌的签发与声明解析组件，使用 {@link Jwtproperties} 中的密钥和毫秒有效时长。
 *
 * <p>签发 HS512 JWT，将本地用户 ID 的字符串写入 subject，并设置签发时间与到期时间。
 *
 * <p>解析时由 JJWT 验证签名及有效期；无效或过期令牌会抛出解析异常。本组件提供令牌操作能力，请求身份校验需由调用方接入。
 */
@Component
public class JwtTokenUtil {

    private final Jwtproperties jwtproperties;

    public JwtTokenUtil(Jwtproperties jwtproperties) {
        this.jwtproperties = jwtproperties;
    }

    /** 根据配置的密钥原文字节构建 HMAC 密钥，长度要求由 JJWT 校验。 */
    private SecretKey getKey() {
        String secret = jwtproperties.getSecret();
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    /**
     * 签发包含用户标识、签发时间和到期时间的 HS512 令牌。
     *
     * @param userId 本地用户 ID 的字符串表示，写入 subject 声明
     * @return 已签名的 JWT 字符串
     */
    public String generateToken(String userId) {
        Date now = new Date();
        long expiration = jwtproperties.getExpiration();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .setSubject(userId)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * 返回配置的令牌有效时长，供登录服务计算响应中的到期时间。
     *
     * @return 有效时长，单位为毫秒
     */
    public Long getExpirationMillis() {
        return jwtproperties.getExpiration();
    }

    /**
     * 验证并解析令牌，读取 subject 中的本地用户 ID。
     *
     * @param token 待解析的 JWT 字符串
     * @return subject 声明中的用户 ID
     * @throws io.jsonwebtoken.JwtException 令牌无效、签名不匹配或已过期
     */
    public String getUserIdFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    /**
     * 解析令牌并将到期时间与当前时间比较。
     *
     * <p>解析过程本身会检查有效期，已过期令牌通常直接抛出异常；调用方不能仅依赖布尔返回值处理过期情况。
     *
     * @param token 待解析的 JWT 字符串
     * @return 解析成功后，到期时间是否早于当前时间
     * @throws io.jsonwebtoken.JwtException 令牌无效或已过期
     */
    public boolean isTokenExpired(String token) {
        Date expiration = getClaimFromToken(token, Claims::getExpiration);
        return expiration.before(new Date());
    }

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
