/**
 * UserFollowRepository
 *
 * 说明：本文件定义了 UserFollowRepository，是 cidi-server 项目中的核心代码文件之一。
 *
 */

package org.daiso.cidi.modules.userfollow.repository;

import org.daiso.cidi.modules.userfollow.entity.UserFollow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserFollowRepository extends JpaRepository<UserFollow, Long> {
}
