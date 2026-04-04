/**
 * LocationImage
 *
 * 说明：本文件定义了 LocationImage，是 cidi-server 项目中的核心代码文件之一。
 *
 */

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
