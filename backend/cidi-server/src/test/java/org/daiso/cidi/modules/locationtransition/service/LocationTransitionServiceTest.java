package org.daiso.cidi.modules.locationtransition.service;

import org.daiso.cidi.modules.location.entity.Location;
import org.daiso.cidi.modules.location.repository.LocationRepository;
import org.daiso.cidi.modules.locationtransition.dto.NextDestinationDTO;
import org.daiso.cidi.modules.locationtransition.entity.LocationTransition;
import org.daiso.cidi.modules.locationtransition.repository.LocationTransitionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocationTransitionServiceTest {
    @Mock private LocationTransitionRepository repository;
    @Mock private LocationRepository locationRepository;
    @InjectMocks private LocationTransitionService service;

    /**
     * 下一目的地预测应采用仓储最新轮次的概率降序前三名，并逐小时展开。
     * 输入跨午夜的预测基准、四个已排序目的地及三小时窗口，预期时间跨日、仅前三名入选且概率百分比四舍五入。
     */
    @Test
    void nextDestinationsExpandsLatestTopThreeAcrossMidnight() {
        LocalDateTime base = LocalDateTime.of(2026, 1, 2, 23, 30);
        when(repository.findLatestBySourceLocation(1L)).thenReturn(List.of(
                transition(2L, "0.425", 42, base), transition(3L, "0.30", 30, base),
                transition(4L, "0.20", 20, base), transition(5L, "0.075", 8, base)));
        when(locationRepository.findById(2L)).thenReturn(Optional.of(Location.builder().id(2L).title("湖畔").build()));
        when(locationRepository.findById(3L)).thenReturn(Optional.empty());
        when(locationRepository.findById(4L)).thenReturn(Optional.of(Location.builder().id(4L).title("山顶").build()));

        List<NextDestinationDTO> result = service.getNextDestinations(1L, 3);

        assertThat(result).extracting(NextDestinationDTO::getHour).containsExactly("23:30", "00:30", "01:30");
        for (NextDestinationDTO dto : result) {
            assertThat(dto.getTransitions()).extracting(NextDestinationDTO.TransitionItem::getLocationId)
                    .containsExactly(2L, 3L, 4L);
            assertThat(dto.getTransitions()).extracting(NextDestinationDTO.TransitionItem::getLocationName)
                    .containsExactly("湖畔", "未知景点", "山顶");
            assertThat(dto.getTransitions()).extracting(NextDestinationDTO.TransitionItem::getRatio)
                    .containsExactly(0.425, 0.30, 0.20);
            assertThat(dto.getTransitions()).extracting(NextDestinationDTO.TransitionItem::getRatioText)
                    .containsExactly("43%", "30%", "20%");
            assertThat(dto.getTransitions()).extracting(NextDestinationDTO.TransitionItem::getUsers)
                    .containsExactly(42, 30, 20);
        }
        assertThat(result.get(0).getTransitions()).isNotSameAs(result.get(1).getTransitions());
        verify(locationRepository, never()).findById(5L);
        verify(repository).findLatestBySourceLocation(1L);
        verifyNoMoreInteractions(repository);
    }

    /**
     * 少于三个目的地时不能补造记录，零和百分之百概率应正确显示。
     * 输入两个目的地及一小时窗口，预期保留两条记录、零人数和百分比边界。
     */
    @Test
    void nextDestinationsHandlesShortListAndProbabilityBounds() {
        LocalDateTime base = LocalDateTime.of(2026, 1, 1, 0, 0);
        when(repository.findLatestBySourceLocation(1L)).thenReturn(List.of(
                transition(2L, "1", 100, base), transition(3L, "0", 0, base)));
        when(locationRepository.findById(2L)).thenReturn(Optional.empty());
        when(locationRepository.findById(3L)).thenReturn(Optional.of(Location.builder().id(3L).title(null).build()));
        List<NextDestinationDTO> result = service.getNextDestinations(1L, 1);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTransitions()).extracting(NextDestinationDTO.TransitionItem::getRatioText)
                .containsExactly("100%", "0%");
        assertThat(result.get(0).getTransitions()).extracting(NextDestinationDTO.TransitionItem::getUsers)
                .containsExactly(100, 0);
        assertThat(result.get(0).getTransitions()).extracting(NextDestinationDTO.TransitionItem::getLocationName)
                .containsExactly("未知景点", "未知景点");
    }

    /**
     * 没有最新转移轮次时应直接返回空预测。
     * 输入空仓储结果和六小时窗口，预期无输出也不查询景点名称。
     */
    @Test
    void nextDestinationsReturnsEmptyWhenNoRoundExists() {
        when(repository.findLatestBySourceLocation(1L)).thenReturn(List.of());
        assertThat(service.getNextDestinations(1L, 6)).isEmpty();
        verifyNoInteractions(locationRepository);
    }

    /**
     * 零小时窗口不应生成任何预测或读取目的地名称。
     * 输入存在预测轮次但窗口为零，预期空列表且不查询名称。
     */
    @Test
    void zeroHourWindowDoesNotGenerateDestinations() {
        when(repository.findLatestBySourceLocation(1L)).thenReturn(List.of(
                transition(2L, "0.5", 5, LocalDateTime.of(2026, 1, 1, 0, 0))));
        assertThat(service.getNextDestinations(1L, 0)).isEmpty();
        verifyNoInteractions(locationRepository);
    }

    /**
     * 预测查询失败不能变成无预测数据。
     * 输入抛出读取异常的仓储，预期原异常向上传播且不查询名称。
     */
    @Test
    void nextDestinationsPropagatesRoundFailure() {
        IllegalStateException failure = new IllegalStateException("预测读取失败");
        when(repository.findLatestBySourceLocation(1L)).thenThrow(failure);
        assertThatThrownBy(() -> service.getNextDestinations(1L, 6)).isSameAs(failure);
        verifyNoInteractions(locationRepository);
    }

    /**
     * 名称查询异常不同于不存在景点，不能一律回退为未知景点。
     * 输入有效预测和抛出异常的名称查询，预期原异常传播。
     */
    @Test
    void nextDestinationsPropagatesLocationLookupFailure() {
        when(repository.findLatestBySourceLocation(1L)).thenReturn(List.of(
                transition(2L, "0.5", 5, LocalDateTime.of(2026, 1, 1, 0, 0))));
        IllegalStateException failure = new IllegalStateException("名称读取失败");
        when(locationRepository.findById(2L)).thenThrow(failure);
        assertThatThrownBy(() -> service.getNextDestinations(1L, 1)).isSameAs(failure);
    }

    /**
     * 全量查询应保持仓储记录次序且不修改实体。
     * 输入两条转移记录，预期返回相同列表并不调用景点仓储。
     */
    @Test
    void findAllReturnsRepositoryResult() {
        List<LocationTransition> rows = List.of(LocationTransition.builder().id(2L).build(),
                LocationTransition.builder().id(1L).build());
        when(repository.findAll()).thenReturn(rows);
        assertThat(service.findAll()).isSameAs(rows);
        verifyNoInteractions(locationRepository);
    }

    /**
     * 单条查询应忠实保留存在和不存在两种结果。
     * 输入存在编号与不存在编号，预期分别返回该实体和空可选值。
     */
    @Test
    void findByIdPreservesPresentAndAbsentResults() {
        LocationTransition row = LocationTransition.builder().id(1L).build();
        when(repository.findById(1L)).thenReturn(Optional.of(row));
        when(repository.findById(9L)).thenReturn(Optional.empty());
        assertThat(service.findById(1L)).containsSame(row);
        assertThat(service.findById(9L)).isEmpty();
    }

    /**
     * 单条保存应使用仓储生成的结果而非直接返回输入。
     * 输入无编号实体及带编号保存结果，预期返回后者并将原实体交给仓储。
     */
    @Test
    void saveReturnsPersistedEntity() {
        LocationTransition input = new LocationTransition();
        LocationTransition saved = LocationTransition.builder().id(8L).build();
        when(repository.save(input)).thenReturn(saved);
        assertThat(service.save(input)).isSameAs(saved);
        verify(repository).save(same(input));
    }

    /**
     * 批量保存只委托仓储，保存结果应完整透传。
     * 输入一个转移预测及带编号结果，预期使用原列表保存并返回仓储列表。
     */
    @Test
    void batchSaveReturnsPersistedList() {
        List<LocationTransition> input = List.of(new LocationTransition());
        List<LocationTransition> saved = List.of(LocationTransition.builder().id(8L).build());
        when(repository.saveAll(input)).thenReturn(saved);
        assertThat(service.batchSave(input)).isSameAs(saved);
        verify(repository).saveAll(same(input));
    }

    /**
     * 空批次不应增加虚构转移记录。
     * 输入空列表，预期仓储接收到空列表且结果仍为空。
     */
    @Test
    void batchSaveHandlesEmptyList() {
        List<LocationTransition> empty = List.of();
        when(repository.saveAll(empty)).thenReturn(empty);
        assertThat(service.batchSave(empty)).isEmpty();
        verify(repository).saveAll(empty);
    }

    /**
     * 删除应精确委托指定编号，不自行读取其他记录。
     * 输入编号七，预期只执行一次仓储删除。
     */
    @Test
    void deleteDelegatesExactId() {
        service.deleteById(7L);
        verify(repository).deleteById(7L);
        verifyNoMoreInteractions(repository);
    }

    /**
     * 各基础操作发生仓储异常时均不得返回成功或空数据。
     * 输入选择的操作及固定仓储异常，预期抛出同一异常实例。
     * @param operation 要验证的查询、保存、批量保存或删除操作
     */
    @ParameterizedTest
    @ValueSource(strings = {"findAll", "findById", "save", "batchSave", "delete"})
    void crudPropagatesRepositoryFailures(String operation) {
        IllegalStateException failure = new IllegalStateException("仓储失败");
        LocationTransition row = new LocationTransition();
        switch (operation) {
            case "findAll" -> {
                when(repository.findAll()).thenThrow(failure);
                assertThatThrownBy(service::findAll).isSameAs(failure);
            }
            case "findById" -> {
                when(repository.findById(1L)).thenThrow(failure);
                assertThatThrownBy(() -> service.findById(1L)).isSameAs(failure);
            }
            case "save" -> {
                when(repository.save(row)).thenThrow(failure);
                assertThatThrownBy(() -> service.save(row)).isSameAs(failure);
            }
            case "batchSave" -> {
                when(repository.saveAll(List.of(row))).thenThrow(failure);
                assertThatThrownBy(() -> service.batchSave(List.of(row))).isSameAs(failure);
            }
            case "delete" -> {
                doThrow(failure).when(repository).deleteById(1L);
                assertThatThrownBy(() -> service.deleteById(1L)).isSameAs(failure);
            }
            default -> throw new IllegalArgumentException(operation);
        }
    }

    private LocationTransition transition(Long targetId, String probability, int users, LocalDateTime hour) {
        return LocationTransition.builder().sourceLocationId(1L).targetLocationId(targetId)
                .predictHour(hour).transitionProbability(new BigDecimal(probability)).expectedUsers(users).build();
    }
}
