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
    @Column(name = "userID")
    private Long userID;

    @Column(name = "Wechat_UnionID")
    private String wechatUnionID;

    @Column(name = "nickname")
    private String nickName;
    @Column(name = "tel_number")
    private String telNumber;
    @Column(name = "signup_time")
    private LocalDateTime signupTime;
    @Column(name = "is_ban")
    private boolean isBan;
}
