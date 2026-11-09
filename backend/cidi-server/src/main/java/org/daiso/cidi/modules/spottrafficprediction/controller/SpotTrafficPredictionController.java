package org.daiso.cidi.modules.spottrafficprediction.controller;

import org.daiso.cidi.core.error.CidiException;
import org.daiso.cidi.core.error.ErrorCode;

import org.daiso.cidi.modules.spottrafficprediction.entity.SpotTrafficPrediction;
import org.daiso.cidi.modules.spottrafficprediction.service.SpotTrafficPredictionService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 景点客流预测 REST 入口，基础路径为 {@code /api/spot-traffic-predictions}。
 *
 * <p>提供实体 CRUD、外部结果批量写入、全局最新预测轮次及最近 21 条记录的密度升序查询。
 *
 * <p>批量写入仅补齐缺失的 createTime，不限制记录数量；最新轮次依据 predictTime，最近记录依据 createTime，具体查询由服务层完成。
 */
@RestController
@RequestMapping("/api/spot-traffic-predictions")
@RequiredArgsConstructor
public class SpotTrafficPredictionController {

    private final SpotTrafficPredictionService service;

    @GetMapping
    public List<SpotTrafficPrediction> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<SpotTrafficPrediction> getById(@PathVariable Long id) {
        return service.findById(id)
            .map(ResponseEntity::ok)
            .orElseThrow(() -> new CidiException(ErrorCode.RESOURCE_NOT_FOUND, "资源不存在: id=" + id));
    }

    @PostMapping
    public SpotTrafficPrediction create(@RequestBody SpotTrafficPrediction entity) {
        return service.save(entity);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SpotTrafficPrediction> update(@PathVariable Long id, @RequestBody SpotTrafficPrediction entity) {
        return service.findById(id)
            .map(existing -> {
                entity.setId(id);
                return ResponseEntity.ok(service.save(entity));
            })
            .orElseThrow(() -> new CidiException(ErrorCode.RESOURCE_NOT_FOUND, "资源不存在: id=" + id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (service.findById(id).isEmpty()) {
            throw new CidiException(ErrorCode.RESOURCE_NOT_FOUND, "资源不存在: id=" + id);
        }
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 接收外部服务的客流预测结果，补齐缺失的 createTime 后保存，不限制每批数量。
     */
    @PostMapping("/batch")
    public ResponseEntity<List<SpotTrafficPrediction>> batchCreate(
            @RequestBody List<SpotTrafficPrediction> entities) {
        entities.forEach(e -> {
            if (e.getCreateTime() == null) {
                e.setCreateTime(java.time.LocalDateTime.now());
            }
        });
        return ResponseEntity.ok(service.batchSave(entities));
    }

    /**
     * 获取最新一轮预测结果（predictTime 最大的那批）
     */
    @GetMapping("/latest")
    public List<SpotTrafficPrediction> getLatest() {
        return service.findLatestRound();
    }

    /**
     * 按 createTime 选取最近最多 21 条记录，再按 crowdDensity 升序返回，空密度排末尾。
     */
    @GetMapping("/recent/crowd-density-asc")
    public List<SpotTrafficPrediction> getRecent21ByCrowdDensityAsc() {
        return service.findRecent21ByCrowdDensityAsc();
    }
}
