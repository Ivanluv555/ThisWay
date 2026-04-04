/**
 * UserFollowDTO
 *
 * 说明：本文件定义了 UserFollowDTO，是 cidi-server 项目中的核心代码文件之一。
 *
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
    private Long followerId;
    private Long followedId;
    private LocalDateTime createTime;

}
