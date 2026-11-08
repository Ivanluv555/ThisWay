package org.daiso.cidi.modules.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;
import org.daiso.cidi.core.error.CidiException;
import org.daiso.cidi.core.error.ErrorCode;
import org.daiso.cidi.core.properties.Wxproperties;
import org.daiso.cidi.core.utils.JwtTokenUtil;
import org.daiso.cidi.core.utils.WeChatApiUtil;
import org.daiso.cidi.modules.user.dto.*;
import org.daiso.cidi.modules.user.entity.User;
import org.daiso.cidi.modules.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WechatAuthServiceTest {
    @Mock private WeChatApiUtil weChatApiUtil;
    @Mock private UserRepository userRepository;
    @Mock private JwtTokenUtil jwtTokenUtil;
    @Mock private Wxproperties wxproperties;
    @InjectMocks private WechatAuthService service;

    /**
     * 验证登录凭证不能为空：输入空引用、空串或纯空白，预期 BAD_REQUEST 且不访问任何依赖。
     *
     * @param code 无效的微信登录凭证
     */
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n"})
    void loginRejectsBlankCode(String code) {
        assertThatThrownBy(() -> service.login(new WechatLoginRequest(code, null, null)))
                .isInstanceOfSatisfying(CidiException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST);
                    assertThat(ex.getMessage()).isEqualTo("code 不能为空");
                });
        verifyNoInteractions(weChatApiUtil, userRepository, jwtTokenUtil, wxproperties);
    }

    /**
     * 验证首次微信登录完成注册和令牌签发：输入有效凭证及个人资料，预期保存激活用户、返回全部 DTO 字段和正确有效期。
     */
    @Test
    void loginCreatesActiveUserAndReturnsTokenAndCompleteDto() {
        when(weChatApiUtil.getSessionInfoFromCode("login-code"))
                .thenReturn(new WeChatApiUtil.WeChatSessionInfo("openid-123456", "union", "secret-session"));
        when(userRepository.findByOpenid("openid-123456")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(23L);
            return user;
        });
        when(jwtTokenUtil.generateToken("23")).thenReturn("signed-token");
        when(jwtTokenUtil.getExpirationMillis()).thenReturn(3_600_000L);
        when(wxproperties.getMiniProgramEnv()).thenReturn("trial");
        LocalDateTime before = LocalDateTime.now();

        WechatLoginResponse response = service.login(new WechatLoginRequest("login-code", "小王", "avatar.png"));

        LocalDateTime after = LocalDateTime.now();
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getOpenid()).isEqualTo("openid-123456");
        assertThat(saved.getUsername()).isEqualTo("小王");
        assertThat(saved.getAvatarUrl()).isEqualTo("avatar.png");
        assertThat(saved.getStatus()).isEqualTo(1);
        assertThat(saved.getCreateTime()).isBetween(before, after);
        assertThat(saved.getLastLoginTime()).isEqualTo(saved.getCreateTime());
        assertThat(response.token()).isEqualTo("signed-token");
        assertThat(response.wechatEnv()).isEqualTo("trial");
        assertThat(response.expiresAt()).isEqualTo(saved.getLastLoginTime().plusHours(1));
        assertThat(response.user()).usingRecursiveComparison().isEqualTo(saved);
        var order = inOrder(userRepository, jwtTokenUtil);
        order.verify(userRepository).findByOpenid("openid-123456");
        order.verify(userRepository).save(saved);
        order.verify(jwtTokenUtil).generateToken("23");
    }

    /**
     * 验证自动昵称的长度边界：输入一位、六位及七位 openid，预期短标识完整保留、长标识只取末六位。
     *
     * @param openid 微信用户标识
     * @param expectedName 自动生成的昵称
     */
    @ParameterizedTest
    @CsvSource({"x,微信用户-x", "123456,微信用户-123456", "0123456,微信用户-123456"})
    void loginBuildsDefaultNicknameAtLengthBoundary(String openid, String expectedName) {
        stubNewLogin(openid);
        WechatLoginResponse response = service.login(new WechatLoginRequest("code", " \t", " "));
        assertThat(response.user().getUsername()).isEqualTo(expectedName);
        assertThat(response.user().getAvatarUrl()).isNull();
    }

    /**
     * 验证默认昵称的防御性分支：输入工具返回空 openid 的模拟会话，预期使用兜底昵称。
     * 正式工具会拒绝此类会话，本测试仅覆盖服务内部的防御性昵称分支，不代表允许无标识注册。
     *
     * @param openid 空白微信标识
     */
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = " ")
    void loginUsesFallbackNicknameForBlankSessionIdentifier(String openid) {
        stubNewLogin(openid);
        assertThat(service.login(new WechatLoginRequest("code", null, null)).user().getUsername())
                .isEqualTo("微信用户");
    }

    /**
     * 验证老用户空资料不会覆盖原资料：输入空昵称和空白头像，预期保留资料、创建时间及状态，只刷新登录时间。
     */
    @Test
    void loginPreservesExistingProfileWhenOptionalFieldsAreBlank() {
        LocalDateTime created = LocalDateTime.of(2024, 1, 2, 3, 4);
        User user = User.builder().id(7L).openid("old-openid").username("原昵称")
                .avatarUrl("old.png").bio("个人简介").phone("13800000000")
                .status(1).createTime(created).lastLoginTime(created).build();
        when(weChatApiUtil.getSessionInfoFromCode("code"))
                .thenReturn(new WeChatApiUtil.WeChatSessionInfo("old-openid", null, null));
        when(userRepository.findByOpenid("old-openid")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(jwtTokenUtil.generateToken("7")).thenReturn("token");
        when(jwtTokenUtil.getExpirationMillis()).thenReturn(1000L);
        LocalDateTime before = LocalDateTime.now();

        WechatLoginResponse response = service.login(new WechatLoginRequest("code", null, " \t"));

        assertThat(user.getUsername()).isEqualTo("原昵称");
        assertThat(user.getAvatarUrl()).isEqualTo("old.png");
        assertThat(user.getCreateTime()).isEqualTo(created);
        assertThat(user.getStatus()).isEqualTo(1);
        assertThat(user.getLastLoginTime()).isBetween(before, LocalDateTime.now());
        assertThat(response.user()).usingRecursiveComparison().isEqualTo(user);
        assertThat(response.user().getPhone()).isEqualTo("13800000000");
        assertThat(response.user().getBio()).isEqualTo("个人简介");
        verify(userRepository).save(same(user));
    }

    /**
     * 验证老用户可同步新资料：输入新昵称和头像，预期更新同一实体且 DTO 反映新资料。
     */
    @Test
    void loginUpdatesExistingNicknameAndAvatar() {
        User user = User.builder().id(7L).openid("openid").username("旧昵称").avatarUrl("old").build();
        when(weChatApiUtil.getSessionInfoFromCode("code"))
                .thenReturn(new WeChatApiUtil.WeChatSessionInfo("openid", null, null));
        when(userRepository.findByOpenid("openid")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(jwtTokenUtil.getExpirationMillis()).thenReturn(1000L);
        WechatLoginResponse result = service.login(new WechatLoginRequest("code", "新昵称", "new.png"));
        assertThat(user.getUsername()).isEqualTo("新昵称");
        assertThat(user.getAvatarUrl()).isEqualTo("new.png");
        assertThat(result.user().getId()).isEqualTo(7L);
        assertThat(result.user().getUsername()).isEqualTo("新昵称");
        assertThat(result.user().getAvatarUrl()).isEqualTo("new.png");
        verify(userRepository).save(same(user));
    }

    /**
     * 验证毫秒有效期转换为整秒：输入不足一秒、整秒及带余数有效期，预期响应截止时间按整秒计算。
     *
     * @param expirationMillis 令牌工具返回的毫秒有效期
     * @param expectedSeconds 截断毫秒余数后的预期秒数
     */
    @ParameterizedTest
    @CsvSource({"0,0", "999,0", "1000,1", "1999,1"})
    void loginConvertsExpirationMillisToWholeSeconds(long expirationMillis, long expectedSeconds) {
        stubNewLogin("abcdef");
        when(jwtTokenUtil.getExpirationMillis()).thenReturn(expirationMillis);
        WechatLoginResponse result = service.login(new WechatLoginRequest("code", null, null));
        assertThat(result.expiresAt()).isEqualTo(result.user().getLastLoginTime().plusSeconds(expectedSeconds));
    }

    /**
     * 验证微信失败会终止登录：输入微信无效凭证异常，预期原样传播且不查询用户或生成令牌。
     */
    @Test
    void loginStopsWhenWechatRejectsCode() {
        CidiException failure = new CidiException(ErrorCode.WECHAT_INVALID_CODE);
        when(weChatApiUtil.getSessionInfoFromCode("bad-code")).thenThrow(failure);
        assertThatThrownBy(() -> service.login(new WechatLoginRequest("bad-code", null, null))).isSameAs(failure);
        verifyNoInteractions(userRepository, jwtTokenUtil, wxproperties);
    }

    /**
     * 验证注册持久化失败不签发令牌：输入保存异常，预期原样传播异常且令牌工具未被调用。
     */
    @Test
    void loginDoesNotIssueTokenAfterSaveFailure() {
        when(weChatApiUtil.getSessionInfoFromCode("code"))
                .thenReturn(new WeChatApiUtil.WeChatSessionInfo("openid", null, null));
        when(userRepository.findByOpenid("openid")).thenReturn(Optional.empty());
        RuntimeException failure = new IllegalStateException("保存失败");
        when(userRepository.save(any(User.class))).thenThrow(failure);
        assertThatThrownBy(() -> service.login(new WechatLoginRequest("code", null, null))).isSameAs(failure);
        verifyNoInteractions(jwtTokenUtil, wxproperties);
    }

    /**
     * 验证签名失败不会返回成功响应：输入令牌生成异常，预期原样传播；单元测试不声称验证事务回滚。
     */
    @Test
    void loginPropagatesTokenGenerationFailure() {
        when(weChatApiUtil.getSessionInfoFromCode("code"))
                .thenReturn(new WeChatApiUtil.WeChatSessionInfo("openid", null, null));
        when(userRepository.findByOpenid("openid")).thenReturn(Optional.of(User.builder().id(8L).build()));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        RuntimeException failure = new IllegalStateException("签名失败");
        when(jwtTokenUtil.generateToken("8")).thenThrow(failure);
        assertThatThrownBy(() -> service.login(new WechatLoginRequest("code", null, null))).isSameAs(failure);
        verify(jwtTokenUtil, never()).getExpirationMillis();
        verifyNoInteractions(wxproperties);
    }

    /**
     * 验证首次绑定手机号：输入未被占用的手机号，预期保存手机号并标记为新绑定。
     */
    @Test
    void bindPhoneBindsPreviouslyUnboundNumber() {
        User user = User.builder().id(1L).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(weChatApiUtil.getPhoneNumberByCode("phone-code")).thenReturn("13800000000");
        when(userRepository.findByPhone("13800000000")).thenReturn(Optional.empty());
        WechatBindPhoneResponse response = service.bindPhone(new WechatBindPhoneRequest(1L, "phone-code"));
        assertThat(response).isEqualTo(new WechatBindPhoneResponse(1L, "13800000000", true));
        assertThat(user.getPhone()).isEqualTo("13800000000");
        verify(userRepository).save(same(user));
        verifyNoInteractions(jwtTokenUtil, wxproperties);
    }

    /**
     * 验证重复绑定本人手机号的幂等含义：输入当前用户已持有的手机号，预期不报冲突且新绑定标记为假。
     */
    @Test
    void bindPhoneAllowsSameOwnerAndReportsNotNewlyBound() {
        User user = User.builder().id(1L).phone("13800000000").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(weChatApiUtil.getPhoneNumberByCode("code")).thenReturn("13800000000");
        when(userRepository.findByPhone("13800000000")).thenReturn(Optional.of(User.builder().id(1L).build()));
        WechatBindPhoneResponse response = service.bindPhone(new WechatBindPhoneRequest(1L, "code"));
        assertThat(response.newlyBound()).isFalse();
        assertThat(response.phone()).isEqualTo("13800000000");
        verify(userRepository).save(user);
    }

    /**
     * 验证用户换绑空闲号码：输入不同于旧号的新手机号，预期替换旧号并标记新绑定。
     */
    @Test
    void bindPhoneReplacesOldPhoneWithAvailableNumber() {
        User user = User.builder().id(1L).phone("13800000000").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(weChatApiUtil.getPhoneNumberByCode("code")).thenReturn("13900000000");
        when(userRepository.findByPhone("13900000000")).thenReturn(Optional.empty());
        assertThat(service.bindPhone(new WechatBindPhoneRequest(1L, "code")).newlyBound()).isTrue();
        assertThat(user.getPhone()).isEqualTo("13900000000");
        verify(userRepository).save(user);
    }

    /**
     * 验证不可绑定他人手机号：输入已归属另一用户的号码，预期 PHONE_ALREADY_BOUND、旧号不变且不保存。
     */
    @Test
    void bindPhoneRejectsNumberOwnedByAnotherUser() {
        User user = User.builder().id(1L).phone("old-phone").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(weChatApiUtil.getPhoneNumberByCode("code")).thenReturn("13800000000");
        when(userRepository.findByPhone("13800000000")).thenReturn(Optional.of(User.builder().id(2L).build()));
        assertThatThrownBy(() -> service.bindPhone(new WechatBindPhoneRequest(1L, "code")))
                .isInstanceOfSatisfying(CidiException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PHONE_ALREADY_BOUND));
        assertThat(user.getPhone()).isEqualTo("old-phone");
        verify(userRepository, never()).save(any());
    }

    /**
     * 验证绑定前用户必须存在：输入不存在的用户标识，预期资源不存在异常且不调用微信手机号接口。
     */
    @Test
    void bindPhoneRejectsMissingUserBeforeCallingWechat() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.bindPhone(new WechatBindPhoneRequest(99L, "code")))
                .isInstanceOfSatisfying(CidiException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
                    assertThat(ex.getMessage()).isEqualTo("用户不存在: id=99");
                });
        verifyNoInteractions(weChatApiUtil, jwtTokenUtil, wxproperties);
        verify(userRepository, never()).save(any());
    }

    /**
     * 验证微信取号失败不破坏原号码：输入手机号接口异常，预期异常传播、原值保持且不查询号码占用或保存。
     */
    @Test
    void bindPhoneStopsAfterWechatFailure() {
        User user = User.builder().id(1L).phone("old").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        CidiException failure = new CidiException(ErrorCode.WECHAT_PHONE_NUMBER_MISSING);
        when(weChatApiUtil.getPhoneNumberByCode("code")).thenThrow(failure);
        assertThatThrownBy(() -> service.bindPhone(new WechatBindPhoneRequest(1L, "code"))).isSameAs(failure);
        assertThat(user.getPhone()).isEqualTo("old");
        verify(userRepository, never()).findByPhone(anyString());
        verify(userRepository, never()).save(any());
    }

    /**
     * 验证手机号保存失败不返回成功结果：输入仓储保存异常，预期原样传播异常。
     */
    @Test
    void bindPhonePropagatesPersistenceFailure() {
        User user = User.builder().id(1L).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(weChatApiUtil.getPhoneNumberByCode("code")).thenReturn("13800000000");
        when(userRepository.findByPhone("13800000000")).thenReturn(Optional.empty());
        RuntimeException failure = new IllegalStateException("保存失败");
        when(userRepository.save(user)).thenThrow(failure);
        assertThatThrownBy(() -> service.bindPhone(new WechatBindPhoneRequest(1L, "code"))).isSameAs(failure);
    }

    private void stubNewLogin(String openid) {
        when(weChatApiUtil.getSessionInfoFromCode("code"))
                .thenReturn(new WeChatApiUtil.WeChatSessionInfo(openid, null, null));
        when(userRepository.findByOpenid(openid)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });
        when(jwtTokenUtil.getExpirationMillis()).thenReturn(1000L);
    }
}
