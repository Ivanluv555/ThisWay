/**
 * UserStatsDTO
 *
 * 说明：用户统计信息DTO
 */

package org.daiso.cidi.modules.userfollow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
