package org.daiso.cidi.modules.user.entity;

import jakarta.persistence.*;

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

    private String nickname;
    private String tel_number;
    private Date signup_time;
    private boolean is_ban;
}
