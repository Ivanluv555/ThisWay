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

/**
 * 热搜关键词的 JPA 持久化实体，映射到 {@code hot_searches} 表。
 *
 * <p>保存关键词、热度值 hotValue 和启用标记 isActive，作为搜索入口的展示数据。
 *
 * <p>当前列表直接查询全部记录，未按热度排序或按启用状态过滤。
 */
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
