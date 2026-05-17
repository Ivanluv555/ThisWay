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

    /**
     * 获取每个景点的最新一条人流记录
     * 用于模拟数据生成服务查询当前状态
     */
    @Query("SELECT f FROM LocationHourlyFlow f WHERE f.recordHour = " +
           "(SELECT MAX(f2.recordHour) FROM LocationHourlyFlow f2 WHERE f2.locationId = f.locationId) " +
           "ORDER BY f.locationId ASC")
    List<LocationHourlyFlow> findLatestForAllLocations();

    /**
     * 获取指定景点的最新一条人流记录
     */
    @Query("SELECT f FROM LocationHourlyFlow f WHERE f.locationId = :locationId " +
           "ORDER BY f.recordHour DESC LIMIT 1")
    List<LocationHourlyFlow> findLatestByLocationId(@Param("locationId") Long locationId);
}
