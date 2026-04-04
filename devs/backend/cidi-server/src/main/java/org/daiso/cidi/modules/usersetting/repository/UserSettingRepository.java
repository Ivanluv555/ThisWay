/**
 * UserSettingRepository
 *
 * 说明：本文件定义了 UserSettingRepository，是 cidi-server 项目中的核心代码文件之一。
 *
 */

package org.daiso.cidi.modules.usersetting.repository;

import org.daiso.cidi.modules.usersetting.entity.UserSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserSettingRepository extends JpaRepository<UserSetting, Long> {
}
