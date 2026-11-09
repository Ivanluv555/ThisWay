package org.daiso.cidi.modules.admin.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 管理员资料的数据传输模型，包含对应记录的标识及业务字段。
 *
 * <p>保存后台人员的岗位、唯一用户名、密码字段、角色字符串和创建时间，主键映射到 adminid。
 *
 * <p>当前 {@code AdminController} 的 CRUD 接口直接收发实体，此 DTO 尚未接入该调用链。
 */
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
