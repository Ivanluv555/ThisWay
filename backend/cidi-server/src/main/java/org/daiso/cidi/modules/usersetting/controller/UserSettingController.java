package org.daiso.cidi.modules.usersetting.controller;

import org.daiso.cidi.core.error.CidiException;
import org.daiso.cidi.core.error.ErrorCode;
import org.daiso.cidi.modules.usersetting.entity.UserSetting;
import org.daiso.cidi.modules.usersetting.service.UserSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户偏好设置 REST 入口，基础路径为 {@code /api/user-settings}。
 *
 * <p>在 {@code /user/{userId}} 提供读取和更新，在其 {@code /reset} 子路径提供重置；输入输出直接使用 UserSetting 实体。
 *
 * <p>首次读取会持久化默认设置；更新仅合并请求体中的非空偏好字段，用户归属由路径参数决定。
 */
@RestController
@RequestMapping("/api/user-settings")
@RequiredArgsConstructor
public class UserSettingController {

    private final UserSettingService service;

    /**
     * 获取用户设置；记录不存在时创建并持久化默认设置。
     * GET /api/user-settings/user/{userId}
     */
    @GetMapping("/user/{userId}")
    public UserSetting getUserSetting(@PathVariable Long userId) {
        return service.getUserSetting(userId);
    }

    /**
     * 合并非空偏好字段；记录不存在时先使用默认值，所属用户由路径参数确定。
     * PUT /api/user-settings/user/{userId}
     */
    @PutMapping("/user/{userId}")
    public UserSetting updateUserSetting(@PathVariable Long userId, @RequestBody UserSetting updates) {
        return service.updateUserSetting(userId, updates);
    }

    /**
     * 重置用户设置为默认值
     * POST /api/user-settings/user/{userId}/reset
     */
    @PostMapping("/user/{userId}/reset")
    public UserSetting resetUserSetting(@PathVariable Long userId) {
        return service.resetUserSetting(userId);
    }
}
