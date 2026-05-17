/**
 * LocationTransitionService
 *
 * 说明：景点间人流转移预测业务逻辑层
 */

package org.daiso.cidi.modules.locationtransition.service;

import lombok.RequiredArgsConstructor;
import org.daiso.cidi.modules.location.repository.LocationRepository;
import org.daiso.cidi.modules.locationtransition.dto.NextDestinationDTO;
import org.daiso.cidi.modules.locationtransition.entity.LocationTransition;
import org.daiso.cidi.modules.locationtransition.repository.LocationTransitionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LocationTransitionService {

    private final LocationTransitionRepository repository;
    private final LocationRepository locationRepository;

    /**
     * 批量保存转移预测数据
     */
    @Transactional
    public List<LocationTransition> batchSave(List<LocationTransition> entities) {
        return repository.saveAll(entities);
    }

    /**
     * 获取某景点的下一目的地预测（未来N小时）
     */
    public List<NextDestinationDTO> getNextDestinations(Long sourceLocationId, int hours) {
        // 获取最新一轮预测
        List<LocationTransition> transitions = repository.findLatestBySourceLocation(sourceLocationId);

        if (transitions.isEmpty()) {
            return Collections.emptyList();
        }

        LocalDateTime baseTime = transitions.get(0).getPredictHour();
        List<NextDestinationDTO> result = new ArrayList<>();

        // 生成未来N小时的预测（基于同一轮数据）
        for (int i = 0; i < hours; i++) {
            LocalDateTime targetHour = baseTime.plusHours(i);
            NextDestinationDTO dto = new NextDestinationDTO();
            dto.setHour(targetHour.format(DateTimeFormatter.ofPattern("HH:mm")));

            // 取Top3转移目标
            List<NextDestinationDTO.TransitionItem> items = transitions.stream()
                    .limit(3)
                    .map(t -> {
                        String locationName = getLocationName(t.getTargetLocationId());
                        NextDestinationDTO.TransitionItem item = new NextDestinationDTO.TransitionItem();
                        item.setLocationId(t.getTargetLocationId());
                        item.setLocationName(locationName);
                        item.setRatio(t.getTransitionProbability().doubleValue());
                        item.setRatioText(Math.round(t.getTransitionProbability().doubleValue() * 100) + "%");
                        item.setUsers(t.getExpectedUsers());
                        return item;
                    })
                    .collect(Collectors.toList());

            dto.setTransitions(items);
            result.add(dto);
        }

        return result;
    }

    /**
     * 根据景点ID获取景点名称
     */
    private String getLocationName(Long locationId) {
        return locationRepository.findById(locationId)
                .map(loc -> loc.getTitle())
                .orElse("未知景点");
    }

    public List<LocationTransition> findAll() {
        return repository.findAll();
    }

    public Optional<LocationTransition> findById(Long id) {
        return repository.findById(id);
    }

    public LocationTransition save(LocationTransition entity) {
        return repository.save(entity);
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
