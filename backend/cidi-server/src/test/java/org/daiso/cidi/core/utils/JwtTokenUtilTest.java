package org.daiso.cidi.core.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import io.jsonwebtoken.security.WeakKeyException;
import org.daiso.cidi.core.properties.Jwtproperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.*;

class JwtTokenUtilTest {
    private static final String SECRET = "0123456789abcdef".repeat(4);
    private Jwtproperties properties;
    private JwtTokenUtil util;

    @BeforeEach
    void setUp() {
        properties = new Jwtproperties();
        properties.setSecret(SECRET);
        properties.setExpiration(3_600_000L);
        util = new JwtTokenUtil(properties);
    }

    /**
     * 确认身份信息、签名算法及有效期完整写入令牌，并可由同一密钥验证。
     *
     * <p>测试输入：用户标识 user-123、64 字节测试密钥、3600000 毫秒有效期。</p>
     * <p>预期结果：生成 HS512 签名令牌，subject 为 user-123；签发与到期时间在调用前后界限内（允许秒精度截断），解析返回相同用户，过期检查为 false。</p>
     */
    @Test
    void generatesSignedTokenWithSubjectAndConfiguredLifetime() {
        long before = System.currentTimeMillis();
        String token = util.generateToken("user-123");
        long after = System.currentTimeMillis();
        var parsed = Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .build().parseClaimsJws(token);
        Claims claims = parsed.getBody();
        assertThat(parsed.getHeader().getAlgorithm()).isEqualTo("HS512");
        assertThat(claims.getSubject()).isEqualTo("user-123");
        // JWT timestamps have second precision.
        assertThat(claims.getIssuedAt().getTime()).isBetween(before - 999, after);
        assertThat(claims.getExpiration().getTime()).isBetween(before + 3_600_000 - 999, after + 3_600_000);
        assertThat(util.getUserIdFromToken(token)).isEqualTo("user-123");
        assertThat(util.isTokenExpired(token)).isFalse();
    }

    /**
     * 确认工具读取当前配置且保持毫秒单位，不使用过时缓存或错误的时间换算。
     *
     * <p>测试输入：先设置有效期 3600000，再将同一配置对象的有效期改为 12345。</p>
     * <p>预期结果：两次 getExpirationMillis() 分别返回 3600000 和 12345。</p>
     */
    @Test
    void exposesConfiguredExpirationInMilliseconds() {
        assertThat(util.getExpirationMillis()).isEqualTo(3_600_000L);
        properties.setExpiration(12_345L);
        assertThat(util.getExpirationMillis()).isEqualTo(12_345L);
    }

    /**
     * 记录当前实现：解析器先拒绝过期令牌，isTokenExpired() 不会返回 true；调用方需处理异常，不应将其误认为布尔判断契约。
     *
     * <p>测试输入：以正确密钥签名、到期时间为当前时间前 60000 毫秒的令牌。</p>
     * <p>预期结果：getUserIdFromToken() 和 isTokenExpired() 均抛出 ExpiredJwtException。</p>
     */
    @Test
    void expiredTokenIsRejectedByParserRatherThanReturningTrue() {
        String token = Jwts.builder().setSubject("expired-user")
                .setExpiration(new Date(System.currentTimeMillis() - 60_000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS512)
                .compact();
        assertThatThrownBy(() -> util.getUserIdFromToken(token)).isInstanceOf(ExpiredJwtException.class);
        assertThatThrownBy(() -> util.isTokenExpired(token)).isInstanceOf(ExpiredJwtException.class);
    }

    /**
     * 拒绝由不受信任密钥签发的身份凭证。
     *
     * <p>测试输入：先用原密钥生成 user-123 的令牌，再切换到另一个 64 字节密钥进行解析。</p>
     * <p>预期结果：用户标识解析和过期检查均抛出 SignatureException。</p>
     */
    @Test
    void rejectsTokenSignedWithAnotherKey() {
        String token = util.generateToken("user-123");
        properties.setSecret("fedcba9876543210".repeat(4));
        assertThatThrownBy(() -> util.getUserIdFromToken(token)).isInstanceOf(SignatureException.class);
        assertThatThrownBy(() -> util.isTokenExpired(token)).isInstanceOf(SignatureException.class);
    }

    /**
     * 防止篡改用户身份后继续复用旧签名冒充其他用户。
     *
     * <p>测试输入：将合法令牌载荷中的 user-123 替换为 admin-456，但保持原签名不变。</p>
     * <p>预期结果：确认载荷已改变；用户标识解析和过期检查均抛出 SignatureException。</p>
     */
    @Test
    void rejectsModifiedPayloadWithoutMatchingSignature() {
        String token = util.generateToken("user-123");
        String[] parts = token.split("\\.");
        String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        String modifiedPayload = payload.replace("user-123", "admin-456");
        assertThat(modifiedPayload).isNotEqualTo(payload);
        String tampered = parts[0] + "." + java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString(modifiedPayload.getBytes(StandardCharsets.UTF_8)) + "." + parts[2];
        assertThatThrownBy(() -> util.getUserIdFromToken(tampered)).isInstanceOf(SignatureException.class);
        assertThatThrownBy(() -> util.isTokenExpired(tampered)).isInstanceOf(SignatureException.class);
    }

    /**
     * 确认有效载荷本身不能代替签名验证，无签名令牌不可作为身份凭证。
     *
     * <p>测试输入：包含 user-123 与未来到期时间、但没有签名的令牌。</p>
     * <p>预期结果：用户标识解析和过期检查均抛出 UnsupportedJwtException。</p>
     */
    @Test
    void rejectsUnsignedToken() {
        String token = Jwts.builder().setSubject("user-123")
                .setExpiration(new Date(System.currentTimeMillis() + 3_600_000)).compact();
        assertThatThrownBy(() -> util.getUserIdFromToken(token)).isInstanceOf(UnsupportedJwtException.class);
        assertThatThrownBy(() -> util.isTokenExpired(token)).isInstanceOf(UnsupportedJwtException.class);
    }

    /**
     * 非法令牌格式应被明确拒绝，不能被当作普通用户标识使用。
     *
     * <p>测试输入：格式不合法的字符串 not-a-jwt。</p>
     * <p>预期结果：用户标识解析和过期检查均抛出 MalformedJwtException。</p>
     */
    @Test
    void rejectsMalformedToken() {
        assertThatThrownBy(() -> util.getUserIdFromToken("not-a-jwt")).isInstanceOf(MalformedJwtException.class);
        assertThatThrownBy(() -> util.isTokenExpired("not-a-jwt")).isInstanceOf(MalformedJwtException.class);
    }

    /**
     * 缺失身份凭证时立即失败，不进入正常认证流程。
     *
     * <p>测试输入：参数化输入分别为 Java null 和空字符串，共 2 组。</p>
     * <p>预期结果：每组输入在用户标识解析和过期检查中均抛出 IllegalArgumentException。</p>
     *
     * @param token 缺失的 JWT 凭证，分别为 null 和空字符串
     */
    @ParameterizedTest
    @NullAndEmptySource
    void rejectsAbsentToken(String token) {
        assertThatThrownBy(() -> util.getUserIdFromToken(token)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> util.isTokenExpired(token)).isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * 防止使用不足以支持指定签名算法的弱密钥签发令牌。
     *
     * <p>测试输入：32 字节密钥及用户标识 user-123，使用 HS512 生成令牌。</p>
     * <p>预期结果：生成过程抛出 WeakKeyException。</p>
     */
    @Test
    void rejectsKeyTooShortForHs512() {
        properties.setSecret("0123456789abcdef".repeat(2));
        assertThatThrownBy(() -> util.generateToken("user-123")).isInstanceOf(WeakKeyException.class);
    }
}
