package org.daiso.cidi.modules.userfollow.service;

import org.daiso.cidi.modules.user.entity.User;
import org.daiso.cidi.modules.user.repository.UserRepository;
import org.daiso.cidi.modules.userfollow.dto.UserFollowDTO;
import org.daiso.cidi.modules.userfollow.dto.UserStatsDTO;
import org.daiso.cidi.modules.userfollow.entity.UserFollow;
import org.daiso.cidi.modules.userfollow.repository.UserFollowRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户关注应用服务，维护有向关系并组装关注列表、粉丝列表及用户统计。
 *
 * <p>专用关注操作查询后复用已有关系，取消关注按用户对删除；列表加载用户资料，并相对 currentUserId 计算两个方向的关系。
 *
 * <p>同时保留直接操作实体的基础 CRUD。当前列表在关联用户缺失时保留 null 元素，禁止关注自己的检查位于控制器的专用关注接口。
 */
@Service
@RequiredArgsConstructor
public class UserFollowService {

    private final UserFollowRepository repository;
    private final UserRepository userRepository;

    public List<UserFollow> findAll() {
        return repository.findAll();
    }

    public Optional<UserFollow> findById(Long id) {
        return repository.findById(id);
    }

    public UserFollow save(UserFollow entity) {
        return repository.save(entity);
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    /**
     * 按关注方向查询已有记录，存在则直接返回，否则创建带当前时间的关系。
     *
     * <p>此处未校验用户存在性或禁止自关注；查询后写入也不构成并发场景下的唯一性保证。
     */
    @Transactional
    public UserFollow followUser(Long followerId, Long followedId) {
        // 检查是否已关注
        Optional<UserFollow> existing = repository.findByFollowerIdAndFollowedId(followerId, followedId);
        if (existing.isPresent()) {
            return existing.get();
        }

        // 创建关注关系
        UserFollow follow = UserFollow.builder()
                .followerId(followerId)
                .followedId(followedId)
                .createTime(LocalDateTime.now())
                .build();
        return repository.save(follow);
    }

    /**
     * 取消关注
     */
    @Transactional
    public void unfollowUser(Long followerId, Long followedId) {
        repository.deleteByFollowerIdAndFollowedId(followerId, followedId);
    }

    /**
     * 检查是否关注
     */
    public boolean isFollowing(Long followerId, Long followedId) {
        return repository.findByFollowerIdAndFollowedId(followerId, followedId).isPresent();
    }

    /**
     * 获取用户的关注列表（我关注的人）
     */
    public List<UserFollowDTO> getFollowings(Long userId, Long currentUserId) {
        List<UserFollow> follows = repository.findFollowingsByUserId(userId);
        return follows.stream()
                .map(follow -> buildUserFollowDTO(follow.getFollowedId(), currentUserId, follow.getCreateTime()))
                .collect(Collectors.toList());
    }

    /**
     * 获取用户的粉丝列表（关注我的人）
     */
    public List<UserFollowDTO> getFollowers(Long userId, Long currentUserId) {
        List<UserFollow> follows = repository.findFollowersByUserId(userId);
        return follows.stream()
                .map(follow -> buildUserFollowDTO(follow.getFollowerId(), currentUserId, follow.getCreateTime()))
                .collect(Collectors.toList());
    }

    /**
     * 获取用户统计信息
     */
    public UserStatsDTO getUserStats(Long userId, Long currentUserId) {
        Long followingCount = repository.countFollowingsByUserId(userId);
        Long followerCount = repository.countFollowersByUserId(userId);
        Boolean isFollowing = currentUserId != null && isFollowing(currentUserId, userId);
        Boolean isFollower = currentUserId != null && isFollowing(userId, currentUserId);

        return UserStatsDTO.builder()
                .userId(userId)
                .followingCount(followingCount)
                .followerCount(followerCount)
                .isFollowing(isFollowing)
                .isFollower(isFollower)
                .build();
    }

    /**
     * 加载被展示用户资料，并相对 currentUserId 计算双向关注状态。
     *
     * @param userId 被展示用户 ID，同时写入 DTO 的 id 和 userId
     * @param currentUserId 关系展示视角的用户 ID，可为空
     * @param followTime 当前列表对应关注记录的创建时间
     * @return 用户展示项，关联用户不存在时返回 null
     */
    private UserFollowDTO buildUserFollowDTO(Long userId, Long currentUserId, LocalDateTime followTime) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return null;
        }

        User user = userOpt.get();
        Boolean isFollowing = currentUserId != null && isFollowing(currentUserId, userId);
        Boolean isFollower = currentUserId != null && isFollowing(userId, currentUserId);

        return UserFollowDTO.builder()
                .id(userId)
                .userId(userId)
                .username(user.getUsername())
                .avatarUrl(user.getAvatarUrl())
                .bio(user.getBio())
                .followTime(followTime)
                .isFollowing(isFollowing)
                .isFollower(isFollower)
                .build();
    }
}
