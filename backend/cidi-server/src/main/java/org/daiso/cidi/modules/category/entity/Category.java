package org.daiso.cidi.modules.category.entity;

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
 * 帖子分类的 JPA 持久化实体，映射到 {@code categories} 表。
 *
 * <p>记录分类名称、图标地址、排序值 sortOrder 和启用标记 isActive，供帖子通过 categoryId 引用。
 *
 * <p>排序值和启用标记仅作为数据存储，当前列表查询未据此排序或过滤。
 */
@Entity
@Table(name = "categories")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Category {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 32)
    private String name;

    @Column(name = "icon_url", length = 255)
    private String iconUrl;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "is_active", nullable = false)
    private Integer isActive;

}
