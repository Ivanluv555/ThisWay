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

@Repository
public interface SpotTrafficPredictionRepository extends JpaRepository<SpotTrafficPrediction, Long> {
}
