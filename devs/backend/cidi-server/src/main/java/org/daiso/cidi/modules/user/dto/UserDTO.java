package org.daiso.cidi.modules.user.dto;

import java.math.BigDecimal;
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
    private String phone;
    private String nickname;
    private String avatarUrl;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime lastLoginTime;

}
