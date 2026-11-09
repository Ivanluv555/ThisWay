package org.daiso.cidi.modules.locationhourlyflow.controller;

import org.daiso.cidi.modules.locationhourlyflow.dto.LocationHourlyFlowDTO;
import org.daiso.cidi.modules.locationhourlyflow.service.LocationHourlyFlowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 景点小时客流 REST 入口，基础路径为 {@code /api/flow/history}。
 *
 * <p>提供默认回溯 168 小时的历史查询、{@code /latest} 最新状态查询和 {@code /batch} 批量写入，均通过 LocationHourlyFlowDTO 交换数据。
 *
 * <p>查询可按 locationId 筛选；批量接口供外部客流数据服务提交记录，当前未限定每批记录数量。
 */
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
     * @return 时间窗口内的历史客流记录
     */
    @GetMapping
    public List<LocationHourlyFlowDTO> getHistory(
            @RequestParam(defaultValue = "168") int hours,
            @RequestParam(required = false) Long locationId) {
        return service.findRecent(hours, locationId);
    }

    /**
     * 接收外部服务批量提交的小时客流，记录数量由调用方决定。
     *
     * @param dtos 待保存的小时客流记录
     * @return 保存后的客流记录，包含持久层返回的主键
     */
    @PostMapping("/batch")
    public ResponseEntity<List<LocationHourlyFlowDTO>> batchSave(
            @RequestBody List<LocationHourlyFlowDTO> dtos) {
        return ResponseEntity.ok(service.batchSave(dtos));
    }

    /**
     * 获取所有景点的最新人流数据（每个景点取最新一条记录）
     * 用于模拟数据生成服务查询当前状态
     */
    @GetMapping("/latest")
    public List<LocationHourlyFlowDTO> getLatestFlows(
            @RequestParam(required = false) Long locationId) {
        return service.findLatest(locationId);
    }
}
