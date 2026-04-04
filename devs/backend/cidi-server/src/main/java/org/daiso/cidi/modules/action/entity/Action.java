/**
 * Action
 *
 * 说明：本文件定义了 Action，是 cidi-server 项目中的核心代码文件之一。
 *
 */

package org.daiso.cidi.modules.action.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "actions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Action {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(nullable = false, length = 16)
    private String type;

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

}
