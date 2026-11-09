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

/**
 * 景点转移预测应用服务，保存外部预测结果并组装下一目的地展示数据。
 *
 * <p>按起点读取最大 predictHour 的记录，取概率最高的三个目标并查询景点名称；景点不存在时显示“未知景点”。
 *
 * <p>从该预测时刻起生成指定数量的小时标签，每个小时复用相同候选、概率及人数；无预测或 hours 非正时返回空列表。
 */
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
     * 将起点最新预测时刻的前三个转移目标展开为按小时组织的展示结果。
     *
     * <p>所有小时使用同一组概率及人数，起始标签为数据中的 predictHour；当前时间不参与筛选。
     *
     * @param sourceLocationId 起点景点 ID
     * @param hours 生成的小时条目数；非正数时返回空列表
     * @return 按小时递增的展示条目，无预测记录时为空列表
     */
    public List<NextDestinationDTO> getNextDestinations(Long sourceLocationId, int hours) {
        // 仓储已按该起点的最大 predictHour 筛选，并按概率降序排列。
        List<LocationTransition> transitions = repository.findLatestBySourceLocation(sourceLocationId);

        if (transitions.isEmpty()) {
            return Collections.emptyList();
        }

        LocalDateTime baseTime = transitions.get(0).getPredictHour();
        List<NextDestinationDTO> result = new ArrayList<>();

        // 复用这一时刻的预测指标，仅将小时标签逐次递增。
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
