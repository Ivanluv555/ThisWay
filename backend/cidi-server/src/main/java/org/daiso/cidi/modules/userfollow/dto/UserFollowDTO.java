package org.daiso.cidi.modules.userfollow.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 关注或粉丝列表中的用户展示项，由 {@code UserFollowService} 组装。
 *
 * <p>{@code id} 和 {@code userId} 当前均为被展示用户的 ID，{@code followTime} 为列表所属用户与该用户之间的关注记录时间。
 *
 * <p>{@code isFollowing} 表示 currentUserId 是否关注此用户，{@code isFollower} 表示此用户是否关注 currentUserId；未传 currentUserId 时两者均为 false。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserFollowDTO {

    private Long id;
    private Long userId;           // 用户ID
    private String username;       // 用户名
    private String avatarUrl;      // 头像
    private String bio;            // 个人简介
    private LocalDateTime followTime;  // 关注时间
    private Boolean isFollowing;   // 我是否关注了TA
    private Boolean isFollower;    // TA是否关注了我
}
