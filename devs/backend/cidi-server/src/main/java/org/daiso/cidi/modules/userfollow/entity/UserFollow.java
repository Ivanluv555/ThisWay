/**
 * UserFollow
 *
 * 说明：本文件定义了 UserFollow，是 cidi-server 项目中的核心代码文件之一。
 *
 */

package org.daiso.cidi.modules.userfollow.entity;

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
@Table(name = "user_follows")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserFollow {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "follower_id", nullable = false)
    private Long followerId;

    @Column(name = "followed_id", nullable = false)
    private Long followedId;

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

}
