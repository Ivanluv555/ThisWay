/**
 * UserDTO
 *
 * 说明：本文件定义了 UserDTO，是 cidi-server 项目中的核心代码文件之一。
 *
 */

package org.daiso.cidi.modules.user.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {


    private Long id;
    private String openid;
    private String username;
    private String phone;
    private String avatarUrl;
    private String bio;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime lastLoginTime;

}
