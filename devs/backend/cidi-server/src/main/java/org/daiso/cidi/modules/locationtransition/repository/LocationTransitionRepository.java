/**
 * LocationTransitionRepository
 *
 * 说明：景点间人流转移预测数据访问层
 */

package org.daiso.cidi.modules.locationtransition.repository;

import org.daiso.cidi.modules.locationtransition.entity.LocationTransition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LocationTransitionRepository extends JpaRepository<LocationTransition, Long> {

    /**
     * 查询某景点最新一轮预测的转移数据（按概率降序）
     */
    @Query("SELECT t FROM LocationTransition t WHERE t.sourceLocationId = :sourceId " +
           "AND t.predictHour = (SELECT MAX(t2.predictHour) FROM LocationTransition t2 " +
           "WHERE t2.sourceLocationId = :sourceId) " +
           "ORDER BY t.transitionProbability DESC")
    List<LocationTransition> findLatestBySourceLocation(@Param("sourceId") Long sourceId);

    /**
     * 查询指定预测时间的所有转移数据
     */
    @Query("SELECT t FROM LocationTransition t WHERE t.predictHour = :predictHour")
    List<LocationTransition> findByPredictHour(@Param("predictHour") LocalDateTime predictHour);
}
