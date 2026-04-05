/**
 * SpotTrafficPredictionRepository
 *
 * 说明：本文件定义了 SpotTrafficPredictionRepository，是 cidi-server 项目中的核心代码文件之一。
 *
 */

package org.daiso.cidi.modules.spottrafficprediction.repository;

import org.daiso.cidi.modules.spottrafficprediction.entity.SpotTrafficPrediction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SpotTrafficPredictionRepository extends JpaRepository<SpotTrafficPrediction, Long> {

    List<SpotTrafficPrediction> findBySpotIdOrderByPredictTimeDesc(Long spotId);

    @org.springframework.data.jpa.repository.Query(
        "SELECT p FROM SpotTrafficPrediction p " +
        "WHERE p.predictTime = (SELECT MAX(q.predictTime) FROM SpotTrafficPrediction q) " +
        "ORDER BY p.spotId ASC")
    List<SpotTrafficPrediction> findLatestRound();

    void deleteBySpotIdIn(java.util.Collection<Long> spotIds);
}
