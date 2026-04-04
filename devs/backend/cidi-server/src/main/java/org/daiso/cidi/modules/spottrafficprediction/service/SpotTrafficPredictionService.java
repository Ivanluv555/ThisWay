/**
 * SpotTrafficPredictionService
 *
 * 说明：本文件定义了 SpotTrafficPredictionService，是 cidi-server 项目中的核心代码文件之一。
 *
 */

package org.daiso.cidi.modules.spottrafficprediction.service;

import org.daiso.cidi.modules.spottrafficprediction.entity.SpotTrafficPrediction;
import org.daiso.cidi.modules.spottrafficprediction.repository.SpotTrafficPredictionRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SpotTrafficPredictionService {

    private final SpotTrafficPredictionRepository repository;

    public List<SpotTrafficPrediction> findAll() {
        return repository.findAll();
    }

    public Optional<SpotTrafficPrediction> findById(Long id) {
        return repository.findById(id);
    }

    public SpotTrafficPrediction save(SpotTrafficPrediction entity) {
        return repository.save(entity);
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
