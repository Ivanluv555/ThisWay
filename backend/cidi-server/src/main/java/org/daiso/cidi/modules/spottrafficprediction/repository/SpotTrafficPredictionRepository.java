package org.daiso.cidi.modules.spottrafficprediction.repository;

import org.daiso.cidi.modules.spottrafficprediction.entity.SpotTrafficPrediction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * 景点客流预测结果的数据访问接口，提供按景点查询、最新轮次和最近写入记录查询。
 *
 * <p>最新轮次是全表最大 predictTime 对应的记录，按 spotId 升序返回；最近写入查询按 createTime 降序并由 Pageable 限量。
 *
 * <p>另提供按一组 spotId 删除记录的方法；最新轮次与最近写入使用不同时间字段。
 */
@Repository
public interface SpotTrafficPredictionRepository extends JpaRepository<SpotTrafficPrediction, Long> {

    List<SpotTrafficPrediction> findBySpotIdOrderByPredictTimeDesc(Long spotId);

    @org.springframework.data.jpa.repository.Query(
        "SELECT p FROM SpotTrafficPrediction p " +
        "WHERE p.predictTime = (SELECT MAX(q.predictTime) FROM SpotTrafficPrediction q) " +
        "ORDER BY p.spotId ASC")
    List<SpotTrafficPrediction> findLatestRound();

    List<SpotTrafficPrediction> findAllByOrderByCreateTimeDesc(Pageable pageable);

    void deleteBySpotIdIn(java.util.Collection<Long> spotIds);
}
