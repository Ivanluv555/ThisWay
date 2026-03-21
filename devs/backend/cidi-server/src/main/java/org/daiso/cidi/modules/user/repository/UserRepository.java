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
