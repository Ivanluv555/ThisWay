package org.daiso.cidi.modules.user.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.daiso.cidi.modules.user.dto.WechatBindPhoneRequest;
import org.daiso.cidi.modules.user.dto.WechatBindPhoneResponse;
import org.daiso.cidi.modules.user.dto.WechatLoginRequest;
import org.daiso.cidi.modules.user.dto.WechatLoginResponse;
import org.daiso.cidi.modules.user.service.WechatAuthService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 微信认证 REST 入口，基础路径为 {@code /api/auth/wechat}。
 *
 * <p>通过 {@code /login} 接收小程序登录 code 并返回本地 JWT 与用户资料，通过 {@code /phone/bind} 接收手机号授权 code 和目标用户 ID。
 *
 * <p>请求先执行 Bean Validation，再委托 {@link WechatAuthService} 完成微信调用、用户写入与手机号冲突检查。
 */
@RestController
@RequestMapping("/api/auth/wechat")
@RequiredArgsConstructor
public class AuthController {

    private final WechatAuthService wechatAuthService;

    @PostMapping("/login")
    public WechatLoginResponse login(@Valid @RequestBody WechatLoginRequest request) {
        return wechatAuthService.login(request);
    }

    @PostMapping("/phone/bind")
    public WechatBindPhoneResponse bindPhone(@Valid @RequestBody WechatBindPhoneRequest request) {
        return wechatAuthService.bindPhone(request);
    }
}
