package org.daiso.cidi.modules.locationtransition.controller;

import lombok.RequiredArgsConstructor;
import org.daiso.cidi.core.error.CidiException;
import org.daiso.cidi.core.error.ErrorCode;
import org.daiso.cidi.modules.locationtransition.dto.NextDestinationDTO;
import org.daiso.cidi.modules.locationtransition.entity.LocationTransition;
import org.daiso.cidi.modules.locationtransition.service.LocationTransitionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 景点转移预测 REST 入口，基础路径为 {@code /api/location-transitions}。
 *
 * <p>除实体 CRUD 外，通过 {@code /batch} 接收外部转移预测并补齐缺失的 createTime，通过下一目的地接口返回按小时组织的展示 DTO。
 *
 * <p>下一目的地默认生成 6 个小时条目，基于起点最新预测时刻的前三个目标；数据选择及展示组装由 LocationTransitionService 完成。
 */
@RestController
@RequestMapping("/api/location-transitions")
@RequiredArgsConstructor
public class LocationTransitionController {

    private final LocationTransitionService service;

    @GetMapping
    public List<LocationTransition> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<LocationTransition> getById(@PathVariable Long id) {
        return service.findById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new CidiException(ErrorCode.RESOURCE_NOT_FOUND, "资源不存在: id=" + id));
    }

    @PostMapping
    public LocationTransition create(@RequestBody LocationTransition entity) {
        return service.save(entity);
    }

    @PutMapping("/{id}")
    public ResponseEntity<LocationTransition> update(@PathVariable Long id, @RequestBody LocationTransition entity) {
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
     * 将起点最新预测时刻的前三个目标展开为小时展示列表。
     *
     * @param sourceLocationId 起点景点 ID
     * @param hours 展示条目数，默认 6，各小时复用相同预测指标
     * @return 下一目的地展示列表
     */
    @GetMapping("/next-destinations/{sourceLocationId}")
    public ResponseEntity<List<NextDestinationDTO>> getNextDestinations(
            @PathVariable Long sourceLocationId,
            @RequestParam(defaultValue = "6") int hours) {
        return ResponseEntity.ok(service.getNextDestinations(sourceLocationId, hours));
    }

    /**
     * 接收外部服务的转移预测，补齐缺失的 createTime 后批量保存。
     */
    @PostMapping("/batch")
    public ResponseEntity<List<LocationTransition>> batchCreate(
            @RequestBody List<LocationTransition> entities) {
        entities.forEach(e -> {
            if (e.getCreateTime() == null) {
                e.setCreateTime(java.time.LocalDateTime.now());
            }
        });
        return ResponseEntity.ok(service.batchSave(entities));
    }
}
