/**
 * UserFollowRepository
 *
 * 说明：本文件定义了 UserFollowRepository，是 cidi-server 项目中的核心代码文件之一。
 */

package org.daiso.cidi.modules.userfollow.repository;

import org.daiso.cidi.modules.userfollow.entity.UserFollow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserFollowRepository extends JpaRepository<UserFollow, Long> {

    /**
     * 查询用户是否关注了某人
     */
    Optional<UserFollow> findByFollowerIdAndFollowedId(Long followerId, Long followedId);

    /**
     * 查询用户的关注列表（我关注的人）
     */
    @Query("SELECT f FROM UserFollow f WHERE f.followerId = :userId ORDER BY f.createTime DESC")
    List<UserFollow> findFollowingsByUserId(@Param("userId") Long userId);

    /**
     * 查询用户的粉丝列表（关注我的人）
     */
    @Query("SELECT f FROM UserFollow f WHERE f.followedId = :userId ORDER BY f.createTime DESC")
    List<UserFollow> findFollowersByUserId(@Param("userId") Long userId);

    /**
     * 统计用户关注数量
     */
    @Query("SELECT COUNT(f) FROM UserFollow f WHERE f.followerId = :userId")
    Long countFollowingsByUserId(@Param("userId") Long userId);

    /**
     * 统计用户粉丝数量
     */
    @Query("SELECT COUNT(f) FROM UserFollow f WHERE f.followedId = :userId")
    Long countFollowersByUserId(@Param("userId") Long userId);

    /**
     * 删除关注关系
     */
    void deleteByFollowerIdAndFollowedId(Long followerId, Long followedId);
}
