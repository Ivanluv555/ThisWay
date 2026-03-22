package org.daiso.cidi.modules.admin.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDTO {


    private Integer id;
    private String job;
    private String username;
    private String password;
    private String role;
    private LocalDateTime createTime;

}
