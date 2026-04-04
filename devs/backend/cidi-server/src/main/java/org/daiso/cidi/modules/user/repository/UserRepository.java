/**
 * UserRepository
 *
 * 说明：本文件定义了 UserRepository，是 cidi-server 项目中的核心代码文件之一。
 *
 */

package org.daiso.cidi.modules.user.repository;

import org.daiso.cidi.modules.user.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
	Optional<User> findByOpenid(String openid);

	Optional<User> findByPhone(String phone);
}
