package org.daiso.cidi.modules.locationhourlyflow.service;

import org.daiso.cidi.modules.locationhourlyflow.dto.LocationHourlyFlowDTO;
import org.daiso.cidi.modules.locationhourlyflow.entity.LocationHourlyFlow;
import org.daiso.cidi.modules.locationhourlyflow.repository.LocationHourlyFlowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 小时客流应用服务，负责历史窗口查询、最新状态读取及 DTO 与实体的转换。
 *
 * <p>历史窗口从服务端当前时间向前回溯指定小时数，可限定单一景点；时间边界与排序规则由对应仓储查询决定。
 *
 * <p>批量写入在事务内执行，空拥挤指数补为零、空来源补为 simulated；保留 DTO 中的 ID，由数据库唯一约束限制重复景点时刻。
 */
@Service
@RequiredArgsConstructor
public class LocationHourlyFlowService {

    private final LocationHourlyFlowRepository repository;

    public List<LocationHourlyFlowDTO> findRecent(int hours, Long locationId) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        List<LocationHourlyFlow> rows = locationId != null
                ? repository.findByLocationIdAndRecordHourAfterOrderByRecordHourAsc(locationId, since)
                : repository.findRecentAll(since);
        return rows.stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional
    public List<LocationHourlyFlowDTO> batchSave(List<LocationHourlyFlowDTO> dtos) {
        List<LocationHourlyFlow> entities = dtos.stream().map(this::toEntity).collect(Collectors.toList());
        return repository.saveAll(entities).stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * 获取每个景点的最新人流数据
     * @param locationId 可选：只查某个景点
     * @return 最新人流记录列表
     */
    public List<LocationHourlyFlowDTO> findLatest(Long locationId) {
        List<LocationHourlyFlow> rows = locationId != null
                ? repository.findLatestByLocationId(locationId)
                : repository.findLatestForAllLocations();
        return rows.stream().map(this::toDTO).collect(Collectors.toList());
    }

    private LocationHourlyFlowDTO toDTO(LocationHourlyFlow e) {
        return LocationHourlyFlowDTO.builder()
                .id(e.getId())
                .locationId(e.getLocationId())
                .recordHour(e.getRecordHour())
                .visitorCount(e.getVisitorCount())
                .congestionIndex(e.getCongestionIndex())
                .dataSource(e.getDataSource())
                .build();
    }

    private LocationHourlyFlow toEntity(LocationHourlyFlowDTO d) {
        return LocationHourlyFlow.builder()
                .id(d.getId())
                .locationId(d.getLocationId())
                .recordHour(d.getRecordHour())
                .visitorCount(d.getVisitorCount())
                .congestionIndex(d.getCongestionIndex() != null ? d.getCongestionIndex() : BigDecimal.ZERO)
                .dataSource(d.getDataSource() != null ? d.getDataSource() : "simulated")
                .build();
    }
}
