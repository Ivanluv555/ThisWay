/**
 * UserFollowService
 *
 * 说明：本文件定义了 UserFollowService，是 cidi-server 项目中的核心代码文件之一。
 */

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
     * 关注用户
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
     * 构建 UserFollowDTO
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
