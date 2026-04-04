/**
 * HotSearch
 *
 * 说明：本文件定义了 HotSearch，是 cidi-server 项目中的核心代码文件之一。
 *
 */

package org.daiso.cidi.modules.hotsearch.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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

    @Column(name = "hot_value", nullable = false)
    private Integer hotValue;

    @Column(name = "is_active", nullable = false)
    private Integer isActive;

}
