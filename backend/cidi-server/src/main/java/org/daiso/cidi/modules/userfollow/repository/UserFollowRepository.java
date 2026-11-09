package org.daiso.cidi.modules.userfollow.repository;

import org.daiso.cidi.modules.userfollow.entity.UserFollow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 有向关注关系的数据访问接口，支持用户对查询、列表、计数及关系删除。
 *
 * <p>关注列表按 followerId 查询、粉丝列表按 followedId 查询，两类列表均按 createTime 降序返回。
 *
 * <p>关系查询通过 {@link Optional} 表达是否存在，计数分别统计出向与入向关系，供主页统计和双向状态展示使用。
 */
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
