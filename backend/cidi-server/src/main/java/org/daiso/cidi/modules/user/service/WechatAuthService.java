package org.daiso.cidi.modules.user.service;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.daiso.cidi.core.error.CidiException;
import org.daiso.cidi.core.error.ErrorCode;
import org.daiso.cidi.core.properties.Wxproperties;
import org.daiso.cidi.core.utils.JwtTokenUtil;
import org.daiso.cidi.core.utils.WeChatApiUtil;
import org.daiso.cidi.modules.user.dto.UserDTO;
import org.daiso.cidi.modules.user.dto.WechatBindPhoneRequest;
import org.daiso.cidi.modules.user.dto.WechatBindPhoneResponse;
import org.daiso.cidi.modules.user.dto.WechatLoginRequest;
import org.daiso.cidi.modules.user.dto.WechatLoginResponse;
import org.daiso.cidi.modules.user.entity.User;
import org.daiso.cidi.modules.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 微信身份与本地用户的应用服务，编排登录、资料同步、JWT 签发和手机号授权绑定。
 *
 * <p>登录时按 openid 查找或创建状态为 1 的用户，仅用非空昵称和头像覆盖资料，更新最后登录时间后签发以本地用户 ID 为 subject 的 JWT。
 *
 * <p>绑定手机号时先确认目标用户存在，再兑换授权手机号并拒绝其他用户已占用的号码；登录和绑定各自在事务内执行。
 */
@Service
@RequiredArgsConstructor
public class WechatAuthService {

    private static final int USER_STATUS_ACTIVE = 1;

    private final WeChatApiUtil weChatApiUtil;
    private final UserRepository userRepository;
    private final JwtTokenUtil jwtTokenUtil;
    private final Wxproperties wxproperties;

    @Transactional
    public WechatLoginResponse login(WechatLoginRequest request) {
        String code = request.code();
        if (!StringUtils.hasText(code)) {
            throw new CidiException(ErrorCode.BAD_REQUEST, "code 不能为空");
        }

        WeChatApiUtil.WeChatSessionInfo sessionInfo = weChatApiUtil.getSessionInfoFromCode(code);
        LocalDateTime now = LocalDateTime.now();

        User user = userRepository.findByOpenid(sessionInfo.openid())
                .orElseGet(() -> User.builder()
                        .openid(sessionInfo.openid())
                        .username(defaultUsername(sessionInfo.openid()))
                        .status(USER_STATUS_ACTIVE)
                        .createTime(now)
                        .build());

        if (StringUtils.hasText(request.nickname())) {
            user.setUsername(request.nickname());
        }
        if (StringUtils.hasText(request.avatarUrl())) {
            user.setAvatarUrl(request.avatarUrl());
        }
        user.setLastLoginTime(now);

        User savedUser = userRepository.save(user);

        String token = jwtTokenUtil.generateToken(String.valueOf(savedUser.getId()));
        LocalDateTime expiresAt = now.plusSeconds(jwtTokenUtil.getExpirationMillis() / 1000);

        return new WechatLoginResponse(
                token,
                expiresAt,
                wxproperties.getMiniProgramEnv(),
                toDto(savedUser)
        );
    }

    @Transactional
    public WechatBindPhoneResponse bindPhone(WechatBindPhoneRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new CidiException(ErrorCode.RESOURCE_NOT_FOUND, "用户不存在: id=" + request.userId()));

        String phone = weChatApiUtil.getPhoneNumberByCode(request.code());

        userRepository.findByPhone(phone)
                .filter(existing -> !existing.getId().equals(user.getId()))
                .ifPresent(existing -> {
                    throw new CidiException(ErrorCode.PHONE_ALREADY_BOUND, "手机号已绑定其他用户");
                });

        boolean newlyBound = !phone.equals(user.getPhone());
        user.setPhone(phone);
        userRepository.save(user);

        return new WechatBindPhoneResponse(user.getId(), phone, newlyBound);
    }

    private UserDTO toDto(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .openid(user.getOpenid())
                .username(user.getUsername())
                .avatarUrl(user.getAvatarUrl())
                .bio(user.getBio())
                .phone(user.getPhone())
                .status(user.getStatus())
                .createTime(user.getCreateTime())
                .lastLoginTime(user.getLastLoginTime())
                .build();
    }

    private String defaultUsername(String openid) {
        if (!StringUtils.hasText(openid)) {
            return "微信用户";
        }
        if (openid.length() <= 6) {
            return "微信用户-" + openid;
        }
        return "微信用户-" + openid.substring(openid.length() - 6);
    }
}
