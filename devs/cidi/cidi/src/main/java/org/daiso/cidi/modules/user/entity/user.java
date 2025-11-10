package org.daiso.cidi.modules.user.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Date;
import lombok.Data;

@Data
@Entity
@Table(name = "users")
public class user {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userID;

    @Id
    private String Wechat_UnionID;

    @Column
    private String nickname;
    @Column
    private String tel_number;
    @Column
    private LocalDateTime signup_time;
    @Column
    private boolean is_ban;
}
