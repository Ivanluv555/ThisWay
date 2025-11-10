package org.daiso.cidi.modules.user.repository;

import org.daiso.cidi.modules.user.entity.user;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface userRepository extends JpaRepository<user,Long> {
    user findByuserID(Long userID);

    Long userID(Long userID);

    List<user> userID(Long userID);
}
