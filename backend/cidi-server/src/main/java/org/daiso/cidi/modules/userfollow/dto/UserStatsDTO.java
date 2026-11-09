package org.daiso.cidi.modules.userfollow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户主页的关注统计与双向关系摘要，由 {@code UserFollowService} 组装。
 *
 * <p>{@code followingCount} 和 {@code followerCount} 分别为目标用户的关注数和粉丝数；两个关系标记相对于 currentUserId 独立计算。
 *
 * <p>{@code isFollowing} 为当前用户关注目标用户，{@code isFollower} 为目标用户关注当前用户；匿名查询时两者均为 false。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatsDTO {

    private Long userId;
    private Long followingCount;   // 关注数
    private Long followerCount;    // 粉丝数
    private Boolean isFollowing;   // 当前用户是否关注了TA
    private Boolean isFollower;    // TA是否关注了当前用户
}
