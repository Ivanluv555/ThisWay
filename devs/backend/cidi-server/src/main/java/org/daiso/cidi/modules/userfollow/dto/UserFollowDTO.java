/**
 * UserFollowDTO
 *
 * 说明：用户关注关系DTO，包含用户详细信息
 */

package org.daiso.cidi.modules.userfollow.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
