package org.daiso.cidi.modules.locationimage.repository;

import org.daiso.cidi.modules.locationimage.entity.LocationImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LocationImageRepository extends JpaRepository<LocationImage, Long> {
}
