/**
 * UserSettingController
 *
 * 说明：本文件定义了 UserSettingController，是 cidi-server 项目中的核心代码文件之一。
 *
 */

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

@RestController
@RequestMapping("/api/user-settings")
@RequiredArgsConstructor
public class UserSettingController {

    private final UserSettingService service;

    /**
     * 获取用户设置（不存在则返回默认设置）
     * GET /api/user-settings/user/{userId}
     */
    @GetMapping("/user/{userId}")
    public UserSetting getUserSetting(@PathVariable Long userId) {
        return service.getUserSetting(userId);
    }

    /**
     * 更新用户设置
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
