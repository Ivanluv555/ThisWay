package org.daiso.cidi.modules.spottrafficprediction.service;

import org.daiso.cidi.modules.spottrafficprediction.entity.SpotTrafficPrediction;
import org.daiso.cidi.modules.spottrafficprediction.repository.SpotTrafficPredictionRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

/**
 * 景点客流预测结果应用服务，封装基础 CRUD、批量保存及结果展示查询。
 *
 * <p>最新轮次沿用仓储对最大 predictTime 的选择；密度查询先取 createTime 最新的最多 21 条，再在内存按 crowdDensity 升序排序，空密度排末尾。
 *
 * <p>本服务读取和保存外部计算结果，批量写入直接委托 saveAll，未限制每批数量。
 */
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

    public List<SpotTrafficPrediction> batchSave(List<SpotTrafficPrediction> entities) {
        return repository.saveAll(entities);
    }

    public List<SpotTrafficPrediction> findLatestRound() {
        return repository.findLatestRound();
    }

    public List<SpotTrafficPrediction> findRecent21ByCrowdDensityAsc() {
        List<SpotTrafficPrediction> recent = repository.findAllByOrderByCreateTimeDesc(PageRequest.of(0, 21));
        recent.sort(Comparator.comparing(SpotTrafficPrediction::getCrowdDensity, Comparator.nullsLast(Comparator.naturalOrder())));
        return recent;
    }
}
