package org.daiso.cidi.modules.action.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 帖子互动记录的数据传输模型，包含对应记录的标识及业务字段。
 *
 * <p>以 userId、postId 和 type 描述用户对帖子的某种操作，createTime 记录操作时间。
 *
 * <p>当前 {@code ActionController} 的 CRUD 接口直接收发实体，此 DTO 尚未接入该调用链。
 */
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
