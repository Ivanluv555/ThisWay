package org.daiso.cidi.modules.locationhourlyflow.repository;

import org.daiso.cidi.modules.locationhourlyflow.entity.LocationHourlyFlow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LocationHourlyFlowRepository extends JpaRepository<LocationHourlyFlow, Long> {

    List<LocationHourlyFlow> findByRecordHourAfterOrderByRecordHourAsc(LocalDateTime since);

    List<LocationHourlyFlow> findByLocationIdAndRecordHourAfterOrderByRecordHourAsc(
            Long locationId, LocalDateTime since);

    @Query("SELECT f FROM LocationHourlyFlow f WHERE f.recordHour >= :since ORDER BY f.locationId ASC, f.recordHour ASC")
    List<LocationHourlyFlow> findRecentAll(@Param("since") LocalDateTime since);
}
