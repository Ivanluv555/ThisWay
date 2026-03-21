package org.daiso.cidi.modules.userfollow.dto;

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
public class UserFollowDTO {


    private Long id;
    private Long followerId;
    private Long followeeId;
    private LocalDateTime createTime;

}
