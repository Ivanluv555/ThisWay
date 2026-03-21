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
