package org.daiso.cidi.modules.usersetting.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.daiso.cidi.modules.usersetting.entity.UserSetting;
import org.daiso.cidi.modules.usersetting.repository.UserSettingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserSettingServiceTest {
    @Mock private UserSettingRepository repository;
    @InjectMocks private UserSettingService service;

    /**
     * 验证读取已有用户设置无副作用：输入存在的自定义设置，预期返回同一实体且不保存或重置。
     */
    @Test
    void getUserSettingReturnsExistingWithoutWriting() {
        UserSetting existing = customSetting();
        when(repository.findByUserId(1L)).thenReturn(Optional.of(existing));
        assertThat(service.getUserSetting(1L)).isSameAs(existing);
        verify(repository).findByUserId(1L);
        verifyNoMoreInteractions(repository);
    }

    /**
     * 验证首次获取设置自动初始化：输入不存在设置的用户，预期保存公开账号、开启通知、系统主题及当前更新时间。
     */
    @Test
    void getUserSettingCreatesAndReturnsPersistedDefaults() {
        when(repository.findByUserId(1L)).thenReturn(Optional.empty());
        UserSetting persisted = UserSetting.builder().userId(1L).isPrivateAccount(0).pushNotifications(1)
                .themeColor("system").build();
        when(repository.save(any())).thenReturn(persisted);
        LocalDateTime before = LocalDateTime.now();
        assertThat(service.getUserSetting(1L)).isSameAs(persisted);
        ArgumentCaptor<UserSetting> captor = ArgumentCaptor.forClass(UserSetting.class);
        verify(repository).save(captor.capture());
        assertDefaults(captor.getValue(), before);
    }

    /**
     * 验证获取设置查询异常不会触发默认创建：输入查询失败，预期原样传播且不保存默认设置。
     */
    @Test
    void getUserSettingStopsAfterLookupFailure() {
        RuntimeException failure = new IllegalStateException("查询失败");
        when(repository.findByUserId(1L)).thenThrow(failure);
        assertThatThrownBy(() -> service.getUserSetting(1L)).isSameAs(failure);
        verify(repository, never()).save(any());
    }

    /**
     * 验证默认设置保存失败可被感知：输入首次读取及仓储保存异常，预期原样传播失败而非返回未保存对象。
     */
    @Test
    void getUserSettingPropagatesDefaultSaveFailure() {
        when(repository.findByUserId(1L)).thenReturn(Optional.empty());
        RuntimeException failure = new IllegalStateException("保存失败");
        when(repository.save(any())).thenThrow(failure);
        assertThatThrownBy(() -> service.getUserSetting(1L)).isSameAs(failure);
    }

    /**
     * 验证完整更新不能更换设置归属或伪造时间：输入不同正文用户标识及过期时间，预期只更新三项业务字段并使用当前时间。
     */
    @Test
    void updateChangesEditableFieldsButPreservesOwnerAndRefreshesTimestamp() {
        UserSetting existing = customSetting();
        when(repository.findByUserId(1L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);
        UserSetting updates = UserSetting.builder().userId(999L).isPrivateAccount(0).pushNotifications(1)
                .themeColor("light").updateTime(LocalDateTime.of(2000, 1, 1, 0, 0)).build();
        LocalDateTime before = LocalDateTime.now();
        assertThat(service.updateUserSetting(1L, updates)).isSameAs(existing);
        assertThat(existing.getUserId()).isEqualTo(1L);
        assertThat(existing.getIsPrivateAccount()).isZero();
        assertThat(existing.getPushNotifications()).isEqualTo(1);
        assertThat(existing.getThemeColor()).isEqualTo("light");
        assertThat(existing.getUpdateTime()).isBetween(before, LocalDateTime.now());
        verify(repository).save(same(existing));
        assertThat(updates.getUserId()).isEqualTo(999L);
        assertThat(updates.getUpdateTime()).isEqualTo(LocalDateTime.of(2000, 1, 1, 0, 0));
    }

    /**
     * 验证部分更新只改变提供的字段：输入单独的隐私、通知或主题值，预期其他字段不变，零值开关不会被忽略。
     *
     * @param field 本次更新的业务字段
     */
    @ParameterizedTest
    @ValueSource(strings = {"privacy", "notifications", "theme"})
    void partialUpdatePreservesOmittedFields(String field) {
        UserSetting existing = UserSetting.builder().userId(1L).isPrivateAccount(1).pushNotifications(1).themeColor("dark").build();
        when(repository.findByUserId(1L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);
        UserSetting updates = new UserSetting();
        if ("privacy".equals(field)) {
            updates.setIsPrivateAccount(0);
        } else if ("notifications".equals(field)) {
            updates.setPushNotifications(0);
        } else {
            updates.setThemeColor("light");
        }
        UserSetting result = service.updateUserSetting(1L, updates);
        assertThat(result.getIsPrivateAccount()).isEqualTo("privacy".equals(field) ? 0 : 1);
        assertThat(result.getPushNotifications()).isEqualTo("notifications".equals(field) ? 0 : 1);
        assertThat(result.getThemeColor()).isEqualTo("theme".equals(field) ? "light" : "dark");
    }

    /**
     * 验证空更新保持已有偏好：输入所有业务字段为空的更新，预期不清空偏好但刷新更新时间并保存。
     */
    @Test
    void emptyUpdatePreservesPreferencesAndRefreshesTime() {
        UserSetting existing = customSetting();
        when(repository.findByUserId(1L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);
        LocalDateTime before = LocalDateTime.now();
        UserSetting result = service.updateUserSetting(1L, new UserSetting());
        assertThat(result.getIsPrivateAccount()).isEqualTo(1);
        assertThat(result.getPushNotifications()).isZero();
        assertThat(result.getThemeColor()).isEqualTo("dark");
        assertThat(result.getUpdateTime()).isBetween(before, LocalDateTime.now());
        verify(repository).save(same(existing));
    }

    /**
     * 验证首次更新只保存最终合并结果：输入不存在设置及单独主题更新，预期其余字段使用默认值且只保存一次。
     */
    @Test
    void updateMissingSettingMergesDefaultsBeforeSingleSave() {
        when(repository.findByUserId(1L)).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        LocalDateTime before = LocalDateTime.now();
        UserSetting result = service.updateUserSetting(1L, UserSetting.builder().userId(999L).themeColor("dark").build());
        assertThat(result.getUserId()).isEqualTo(1L);
        assertThat(result.getIsPrivateAccount()).isZero();
        assertThat(result.getPushNotifications()).isEqualTo(1);
        assertThat(result.getThemeColor()).isEqualTo("dark");
        assertThat(result.getUpdateTime()).isBetween(before, LocalDateTime.now());
        verify(repository, times(1)).save(same(result));
    }

    /**
     * 验证首次空更新产生完整默认设置：输入不存在设置和空更新，预期所有默认字段齐全并有当前更新时间。
     */
    @Test
    void emptyUpdateOfMissingSettingCreatesDefaults() {
        when(repository.findByUserId(1L)).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        LocalDateTime before = LocalDateTime.now();
        assertDefaults(service.updateUserSetting(1L, new UserSetting()), before);
    }

    /**
     * 验证更新保存失败不返回成功：输入有效更新及保存异常，预期原样传播；不以 mock 测试声称事务回滚。
     */
    @Test
    void updatePropagatesSaveFailure() {
        UserSetting existing = customSetting();
        when(repository.findByUserId(1L)).thenReturn(Optional.of(existing));
        RuntimeException failure = new IllegalStateException("保存失败");
        when(repository.save(existing)).thenThrow(failure);
        assertThatThrownBy(() -> service.updateUserSetting(1L, UserSetting.builder().themeColor("light").build()))
                .isSameAs(failure);
    }

    /**
     * 验证重置删除旧值后再创建默认值：输入用户标识，预期按顺序删除并保存默认设置，不额外读取旧设置。
     */
    @Test
    void resetDeletesBeforeSavingDefaults() {
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        LocalDateTime before = LocalDateTime.now();
        UserSetting result = service.resetUserSetting(1L);
        assertDefaults(result, before);
        var order = inOrder(repository);
        order.verify(repository).deleteByUserId(1L);
        order.verify(repository).save(same(result));
        order.verifyNoMoreInteractions();
    }

    /**
     * 验证重置在删除失败后停止：输入删除异常，预期原样传播且不覆盖或创建默认设置。
     */
    @Test
    void resetDoesNotSaveIfDeletionFails() {
        RuntimeException failure = new IllegalStateException("删除失败");
        doThrow(failure).when(repository).deleteByUserId(1L);
        assertThatThrownBy(() -> service.resetUserSetting(1L)).isSameAs(failure);
        verify(repository, never()).save(any());
    }

    /**
     * 验证重置保存默认值失败不报告成功：输入删除成功后的保存异常，预期原样传播；事务效果留待集成测试。
     */
    @Test
    void resetPropagatesDefaultSaveFailure() {
        RuntimeException failure = new IllegalStateException("保存失败");
        when(repository.save(any())).thenThrow(failure);
        assertThatThrownBy(() -> service.resetUserSetting(1L)).isSameAs(failure);
        var order = inOrder(repository);
        order.verify(repository).deleteByUserId(1L);
        order.verify(repository).save(any(UserSetting.class));
    }

    /**
     * 验证基础设置读取委托：输入已有设置列表及标识，预期返回仓储原始结果而不触发默认创建。
     */
    @Test
    void basicReadsReturnRepositoryResults() {
        UserSetting setting = customSetting();
        List<UserSetting> settings = List.of(setting);
        when(repository.findAll()).thenReturn(settings);
        when(repository.findById(1L)).thenReturn(Optional.of(setting));
        assertThat(service.findAll()).isSameAs(settings);
        assertThat(service.findById(1L)).containsSame(setting);
        verify(repository).findAll();
        verify(repository).findById(1L);
        verifyNoMoreInteractions(repository);
    }

    /**
     * 验证基础查询不存在时不创建设置：输入空列表和空 Optional，预期保持空结果且不保存默认值。
     */
    @Test
    void basicReadsPreserveEmptyResults() {
        when(repository.findAll()).thenReturn(List.of());
        when(repository.findById(1L)).thenReturn(Optional.empty());
        assertThat(service.findAll()).isEmpty();
        assertThat(service.findById(1L)).isEmpty();
        verify(repository, never()).save(any());
    }

    /**
     * 验证基础保存返回实际仓储结果：输入设置草稿及不同的保存实体，预期返回后者而非直接返回输入。
     */
    @Test
    void saveReturnsRepositoryEntity() {
        UserSetting draft = new UserSetting();
        UserSetting saved = customSetting();
        when(repository.save(draft)).thenReturn(saved);
        assertThat(service.save(draft)).isSameAs(saved);
        verify(repository).save(same(draft));
    }

    /**
     * 验证基础删除按主键操作：输入用户设置主键，预期调用 deleteById 且不混用重置的 deleteByUserId。
     */
    @Test
    void deleteByIdDelegatesWithoutResetting() {
        service.deleteById(1L);
        verify(repository).deleteById(1L);
        verifyNoMoreInteractions(repository);
    }

    /**
     * 验证基础删除异常不被吞掉：输入仓储删除失败，预期原样传播异常。
     */
    @Test
    void deleteByIdPropagatesFailure() {
        RuntimeException failure = new IllegalStateException("删除失败");
        doThrow(failure).when(repository).deleteById(1L);
        assertThatThrownBy(() -> service.deleteById(1L)).isSameAs(failure);
    }

    private UserSetting customSetting() {
        return UserSetting.builder().userId(1L).isPrivateAccount(1).pushNotifications(0)
                .themeColor("dark").updateTime(LocalDateTime.of(2024, 1, 1, 0, 0)).build();
    }

    private void assertDefaults(UserSetting setting, LocalDateTime before) {
        assertThat(setting.getUserId()).isEqualTo(1L);
        assertThat(setting.getIsPrivateAccount()).isZero();
        assertThat(setting.getPushNotifications()).isEqualTo(1);
        assertThat(setting.getThemeColor()).isEqualTo("system");
        assertThat(setting.getUpdateTime()).isBetween(before, LocalDateTime.now());
    }
}
