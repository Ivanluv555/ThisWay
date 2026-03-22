package org.daiso.cidi.modules.action.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActionDTO {


    private Long id;
    private Long userId;
    private Long postId;
    private String type;
    private LocalDateTime createTime;

}
