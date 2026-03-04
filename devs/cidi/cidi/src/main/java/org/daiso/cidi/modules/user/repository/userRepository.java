package org.daiso.cidi.modules.user.repository;

import org.daiso.cidi.modules.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByOpenid(String openid);
}