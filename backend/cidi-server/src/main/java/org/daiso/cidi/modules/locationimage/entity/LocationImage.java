package org.daiso.cidi.modules.locationimage.entity;

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
 * 景点图片元数据的 JPA 持久化实体，映射到 {@code location_images} 表。
 *
 * <p>以 locationId 标识所属景点，保存图片 URL、封面标记 isCover 和展示顺序 sortOrder。
 *
 * <p>本实体保存图片地址及展示属性，当前代码未约束每个景点只能有一张封面。
 */
@Entity
@Table(name = "location_images")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationImage {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "location_id", nullable = false)
    private Long locationId;

    @Column(name = "image_url", nullable = false, length = 255)
    private String imageUrl;

    @Column(name = "is_cover", nullable = false)
    private Integer isCover;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

}
