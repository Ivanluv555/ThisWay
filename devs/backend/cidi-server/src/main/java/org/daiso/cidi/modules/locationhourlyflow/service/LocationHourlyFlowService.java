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
