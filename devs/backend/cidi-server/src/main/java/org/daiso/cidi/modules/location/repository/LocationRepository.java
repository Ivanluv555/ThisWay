/**
 * LocationRepository
 *
 * 说明：本文件定义了 LocationRepository，是 cidi-server 项目中的核心代码文件之一。
 *
 */

package org.daiso.cidi.modules.location.repository;

import org.daiso.cidi.modules.location.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LocationRepository extends JpaRepository<Location, Long> {
}
