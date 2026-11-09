package org.daiso.cidi.modules.user.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 小程序用户资料的数据传输模型，包含对应记录的标识及业务字段。
 *
 * <p>以唯一 openid 关联微信身份，保存昵称、头像、简介、可选唯一手机号、状态和登录时间。
 *
 * <p>由 {@code WechatAuthService} 组装后嵌入微信登录响应；通用用户 CRUD 接口直接使用实体。
 */
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
