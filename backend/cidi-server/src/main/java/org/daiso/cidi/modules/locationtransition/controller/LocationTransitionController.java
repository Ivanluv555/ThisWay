/**
 * LocationTransitionController
 *
 * 说明：景点间人流转移预测控制器
 */

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
     * 获取某景点的下一目的地预测（最新一轮）
     */
    @GetMapping("/next-destinations/{sourceLocationId}")
    public ResponseEntity<List<NextDestinationDTO>> getNextDestinations(
            @PathVariable Long sourceLocationId,
            @RequestParam(defaultValue = "6") int hours) {
        return ResponseEntity.ok(service.getNextDestinations(sourceLocationId, hours));
    }

    /**
     * 批量写入转移预测（供FastAPI调用）
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
