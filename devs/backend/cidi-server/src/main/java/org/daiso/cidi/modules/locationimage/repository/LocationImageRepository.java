/**
 * LocationImageRepository
 *
 * 说明：本文件定义了 LocationImageRepository，是 cidi-server 项目中的核心代码文件之一。
 *
 */

package org.daiso.cidi.modules.locationimage.repository;

import org.daiso.cidi.modules.locationimage.entity.LocationImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LocationImageRepository extends JpaRepository<LocationImage, Long> {
}
