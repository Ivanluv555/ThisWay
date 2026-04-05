package org.daiso.cidi.modules.locationhourlyflow.controller;

import org.daiso.cidi.modules.locationhourlyflow.dto.LocationHourlyFlowDTO;
import org.daiso.cidi.modules.locationhourlyflow.service.LocationHourlyFlowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/flow/history")
@RequiredArgsConstructor
public class LocationHourlyFlowController {

    private final LocationHourlyFlowService service;

    /**
     * 获取历史人流数据
     *
     * @param hours      向前追溯的小时数，默认 168（7天）
     * @param locationId 可选：只查某个景点
     */
    @GetMapping
    public List<LocationHourlyFlowDTO> getHistory(
            @RequestParam(defaultValue = "168") int hours,
            @RequestParam(required = false) Long locationId) {
        return service.findRecent(hours, locationId);
    }

    /**
     * 批量写入历史人流（FastAPI 每小时调用，每次写 21 条）
     */
    @PostMapping("/batch")
    public ResponseEntity<List<LocationHourlyFlowDTO>> batchSave(
            @RequestBody List<LocationHourlyFlowDTO> dtos) {
        return ResponseEntity.ok(service.batchSave(dtos));
    }
}
