/**
 * UserSettingService
 *
 * 说明：本文件定义了 UserSettingService，是 cidi-server 项目中的核心代码文件之一。
 */

package org.daiso.cidi.modules.usersetting.service;

import org.daiso.cidi.modules.usersetting.entity.UserSetting;
import org.daiso.cidi.modules.usersetting.repository.UserSettingRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserSettingService {

    private final UserSettingRepository repository;

    public List<UserSetting> findAll() {
        return repository.findAll();
    }

    public Optional<UserSetting> findById(Long id) {
        return repository.findById(id);
    }

    public UserSetting save(UserSetting entity) {
        return repository.save(entity);
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    /**
     * 获取用户设置（不存在则创建默认设置）
     */
    public UserSetting getUserSetting(Long userId) {
        return repository.findByUserId(userId)
                .orElseGet(() -> createDefaultSetting(userId));
    }

    /**
     * 更新用户设置
     */
    @Transactional
    public UserSetting updateUserSetting(Long userId, UserSetting updates) {
        UserSetting setting = repository.findByUserId(userId)
                .orElseGet(() -> {
                    UserSetting newSetting = UserSetting.builder()
                            .userId(userId)
                            .isPrivateAccount(0)
                            .pushNotifications(1)
                            .themeColor("system")
                            .updateTime(LocalDateTime.now())
                            .build();
                    return newSetting;
                });

        // 更新字段
        if (updates.getIsPrivateAccount() != null) {
            setting.setIsPrivateAccount(updates.getIsPrivateAccount());
        }
        if (updates.getPushNotifications() != null) {
            setting.setPushNotifications(updates.getPushNotifications());
        }
        if (updates.getThemeColor() != null) {
            setting.setThemeColor(updates.getThemeColor());
        }
        setting.setUpdateTime(LocalDateTime.now());

        return repository.save(setting);
    }

    /**
     * 重置用户设置为默认值
     */
    @Transactional
    public UserSetting resetUserSetting(Long userId) {
        repository.deleteByUserId(userId);
        return createDefaultSetting(userId);
    }

    /**
     * 创建默认设置
     */
    private UserSetting createDefaultSetting(Long userId) {
        UserSetting setting = UserSetting.builder()
                .userId(userId)
                .isPrivateAccount(0)
                .pushNotifications(1)
                .themeColor("system")
                .updateTime(LocalDateTime.now())
                .build();
        return repository.save(setting);
    }
}
