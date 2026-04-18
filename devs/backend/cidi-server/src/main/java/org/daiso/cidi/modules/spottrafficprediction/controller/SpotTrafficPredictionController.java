/**
 * SpotTrafficPredictionController
 *
 * 说明：本文件定义了 SpotTrafficPredictionController，是 cidi-server 项目中的核心代码文件之一。
 *
 */

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
     * 批量写入预测结果（供 FastAPI 调用，每次写 21 条）
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
     * 查询最近 21 条预测记录，并按 crowdDensity 从小到大返回
     */
    @GetMapping("/recent/crowd-density-asc")
    public List<SpotTrafficPrediction> getRecent21ByCrowdDensityAsc() {
        return service.findRecent21ByCrowdDensityAsc();
    }
}
