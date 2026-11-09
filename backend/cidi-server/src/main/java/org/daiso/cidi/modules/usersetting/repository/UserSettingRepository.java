package org.daiso.cidi.modules.usersetting.repository;

import org.daiso.cidi.modules.usersetting.entity.UserSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/**
 * 用户偏好设置的 Spring Data JPA 数据访问接口。
 *
 * <p>实体以 userId 为主键，除继承的主键 CRUD 外，还提供按用户 ID 查询、判断存在和删除的方法。
 *
 * <p>供设置服务实现按需创建默认值、合并更新及删除后重建的重置流程。
 */
@Repository
public interface UserSettingRepository extends JpaRepository<UserSetting, Long> {
    Optional<UserSetting> findByUserId(Long userId);
    boolean existsByUserId(Long userId);
    void deleteByUserId(Long userId);
}
