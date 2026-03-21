package org.daiso.cidi.modules.userfollow.repository;

import org.daiso.cidi.modules.userfollow.entity.UserFollow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserFollowRepository extends JpaRepository<UserFollow, Long> {
}
