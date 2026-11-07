/**
 * UserFollowController
 *
 * 说明：本文件定义了 UserFollowController，是 cidi-server 项目中的核心代码文件之一。
 */

package org.daiso.cidi.modules.userfollow.controller;

import org.daiso.cidi.core.error.CidiException;
import org.daiso.cidi.core.error.ErrorCode;

import org.daiso.cidi.modules.userfollow.dto.UserFollowDTO;
import org.daiso.cidi.modules.userfollow.dto.UserStatsDTO;
import org.daiso.cidi.modules.userfollow.entity.UserFollow;
import org.daiso.cidi.modules.userfollow.service.UserFollowService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user-follows")
@RequiredArgsConstructor
public class UserFollowController {

    private final UserFollowService service;

    @GetMapping
    public List<UserFollow> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserFollow> getById(@PathVariable Long id) {
        return service.findById(id)
            .map(ResponseEntity::ok)
            .orElseThrow(() -> new CidiException(ErrorCode.RESOURCE_NOT_FOUND, "资源不存在: id=" + id));
    }

    @PostMapping
    public UserFollow create(@RequestBody UserFollow entity) {
        return service.save(entity);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserFollow> update(@PathVariable Long id, @RequestBody UserFollow entity) {
        return service.findById(id)
            .map(existing -> {
                entity.setId(id);
                return ResponseEntity.ok(service.save(entity));
            })
            .orElseThrow(() -> new CidiException(ErrorCode.RESOURCE_NOT_FOUND, "资源不存在: id=" + id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (service.findById(id).isEmpty()) {
            throw new CidiException(ErrorCode.RESOURCE_NOT_FOUND, "资源不存在: id=" + id);
        }
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 关注用户
     */
    @PostMapping("/follow")
    public ResponseEntity<UserFollow> followUser(
            @RequestParam Long followerId,
            @RequestParam Long followedId) {
        if (followerId.equals(followedId)) {
            throw new CidiException(ErrorCode.BAD_REQUEST, "不能关注自己");
        }
        return ResponseEntity.ok(service.followUser(followerId, followedId));
    }

    /**
     * 取消关注
     */
    @PostMapping("/unfollow")
    public ResponseEntity<Void> unfollowUser(
            @RequestParam Long followerId,
            @RequestParam Long followedId) {
        service.unfollowUser(followerId, followedId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 检查是否关注
     */
    @GetMapping("/is-following")
    public ResponseEntity<Boolean> isFollowing(
            @RequestParam Long followerId,
            @RequestParam Long followedId) {
        return ResponseEntity.ok(service.isFollowing(followerId, followedId));
    }

    /**
     * 获取用户的关注列表（我关注的人）
     */
    @GetMapping("/followings/{userId}")
    public ResponseEntity<List<UserFollowDTO>> getFollowings(
            @PathVariable Long userId,
            @RequestParam(required = false) Long currentUserId) {
        return ResponseEntity.ok(service.getFollowings(userId, currentUserId));
    }

    /**
     * 获取用户的粉丝列表（关注我的人）
     */
    @GetMapping("/followers/{userId}")
    public ResponseEntity<List<UserFollowDTO>> getFollowers(
            @PathVariable Long userId,
            @RequestParam(required = false) Long currentUserId) {
        return ResponseEntity.ok(service.getFollowers(userId, currentUserId));
    }

    /**
     * 获取用户统计信息
     */
    @GetMapping("/stats/{userId}")
    public ResponseEntity<UserStatsDTO> getUserStats(
            @PathVariable Long userId,
            @RequestParam(required = false) Long currentUserId) {
        return ResponseEntity.ok(service.getUserStats(userId, currentUserId));
    }
}
