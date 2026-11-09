package org.daiso.cidi.modules.locationtransition.repository;

import org.daiso.cidi.modules.locationtransition.entity.LocationTransition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 景点间转移预测的数据访问接口，支持按起点选取最新时刻以及按预测时刻查询。
 *
 * <p>最新查询使用该起点的最大 predictHour 筛选记录，并按 transitionProbability 降序返回；createTime 不参与这一选择。
 *
 * <p>最新记录的排序供服务层截取前三个候选目的地，基础写入由继承的 JPA 操作提供。
 */
@Repository
public interface LocationTransitionRepository extends JpaRepository<LocationTransition, Long> {

    /**
     * 查询起点最大 predictHour 对应的全部转移记录，并按概率降序返回。
     *
     * @param sourceId 起点景点 ID
     * @return 最新预测时刻的候选目标；无记录时为空列表
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
