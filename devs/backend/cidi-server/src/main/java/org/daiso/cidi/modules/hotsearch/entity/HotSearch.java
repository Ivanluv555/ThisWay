package org.daiso.cidi.modules.hotsearch.entity;

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
@Table(name = "hot_searches")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HotSearch {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 64)
    private String keyword;

    @Column(name = "scount", nullable = false)
    private Integer scount;

    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

}
