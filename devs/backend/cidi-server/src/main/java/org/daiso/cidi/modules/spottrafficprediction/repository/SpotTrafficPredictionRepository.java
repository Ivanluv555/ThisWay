package org.daiso.cidi.modules.spottrafficprediction.repository;

import org.daiso.cidi.modules.spottrafficprediction.entity.SpotTrafficPrediction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpotTrafficPredictionRepository extends JpaRepository<SpotTrafficPrediction, Long> {
}
