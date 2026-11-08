package org.daiso.cidi.modules.locationhourlyflow.service;

import org.daiso.cidi.modules.locationhourlyflow.dto.LocationHourlyFlowDTO;
import org.daiso.cidi.modules.locationhourlyflow.entity.LocationHourlyFlow;
import org.daiso.cidi.modules.locationhourlyflow.repository.LocationHourlyFlowRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocationHourlyFlowServiceTest {
    @Mock private LocationHourlyFlowRepository repository;
    @InjectMocks private LocationHourlyFlowService service;

    /**
     * 指定景点查询历史客流时应使用景点过滤查询。
     * 输入景点编号及二十四小时窗口，预期截止时间处于调用前后计算区间，全部业务字段正确映射。
     */
    @Test
    void findRecentFiltersLocationAndMapsEveryField() {
        LocationHourlyFlow row = row(7L, 2L);
        when(repository.findByLocationIdAndRecordHourAfterOrderByRecordHourAsc(eq(2L), any()))
                .thenReturn(List.of(row));
        LocalDateTime before = LocalDateTime.now().minusHours(24);

        List<LocationHourlyFlowDTO> result = service.findRecent(24, 2L);

        LocalDateTime after = LocalDateTime.now().minusHours(24);
        ArgumentCaptor<LocalDateTime> since = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(repository).findByLocationIdAndRecordHourAfterOrderByRecordHourAsc(eq(2L), since.capture());
        assertThat(since.getValue()).isBetween(before, after);
        assertThat(result).singleElement().satisfies(dto -> {
            assertThat(dto.getId()).isEqualTo(row.getId());
            assertThat(dto.getLocationId()).isEqualTo(row.getLocationId());
            assertThat(dto.getRecordHour()).isEqualTo(row.getRecordHour());
            assertThat(dto.getVisitorCount()).isEqualTo(120);
            assertThat(dto.getCongestionIndex()).isEqualByComparingTo("0.75");
            assertThat(dto.getDataSource()).isEqualTo("sensor");
        });
        verifyNoMoreInteractions(repository);
    }

    /**
     * 未指定景点时应使用全景点历史查询并保留仓储排序。
     * 输入七天窗口及两个已排序记录，预期输出编号顺序不变且不调用单景点查询。
     */
    @Test
    void findRecentAllPreservesRepositoryOrder() {
        when(repository.findRecentAll(any())).thenReturn(List.of(row(5L, 1L), row(3L, 2L)));
        assertThat(service.findRecent(168, null)).extracting(LocationHourlyFlowDTO::getId)
                .containsExactly(5L, 3L);
        verify(repository).findRecentAll(any());
        verifyNoMoreInteractions(repository);
    }

    /**
     * 零小时窗口应从当前时间开始查询，没有历史记录时返回空集合。
     * 输入零小时及空仓储结果，预期截止时间为调用期间当前时间，不生成虚构数据。
     */
    @Test
    void findRecentZeroHoursReturnsEmptyResult() {
        when(repository.findRecentAll(any())).thenReturn(List.of());
        LocalDateTime before = LocalDateTime.now();
        assertThat(service.findRecent(0, null)).isEmpty();
        LocalDateTime after = LocalDateTime.now();
        ArgumentCaptor<LocalDateTime> since = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(repository).findRecentAll(since.capture());
        assertThat(since.getValue()).isBetween(before, after);
    }

    /**
     * 指定景点的最新状态查询不应退化为历史查询。
     * 输入单景点编号及最新记录，预期返回该记录并只调用对应最新查询。
     */
    @Test
    void findLatestUsesLocationQuery() {
        when(repository.findLatestByLocationId(4L)).thenReturn(List.of(row(9L, 4L)));
        assertThat(service.findLatest(4L)).extracting(LocationHourlyFlowDTO::getId).containsExactly(9L);
        verify(repository).findLatestByLocationId(4L);
        verifyNoMoreInteractions(repository);
    }

    /**
     * 全景点最新状态应保留仓储按景点编号给出的顺序。
     * 输入空景点过滤及两条最新记录，预期返回两条映射结果且仅查询全景点最新轮次。
     */
    @Test
    void findLatestAllPreservesOrder() {
        when(repository.findLatestForAllLocations()).thenReturn(List.of(row(9L, 1L), row(8L, 2L)));
        assertThat(service.findLatest(null)).extracting(LocationHourlyFlowDTO::getLocationId)
                .containsExactly(1L, 2L);
        verify(repository).findLatestForAllLocations();
        verifyNoMoreInteractions(repository);
    }

    /**
     * 尚无最新数据的景点不应返回占位记录。
     * 输入不存在记录的景点编号，预期返回空列表。
     */
    @Test
    void findLatestReturnsEmptyForUnknownLocation() {
        when(repository.findLatestByLocationId(99L)).thenReturn(List.of());
        assertThat(service.findLatest(99L)).isEmpty();
    }

    /**
     * 批量写入时应补齐缺省拥挤指数和数据来源，同时保留显式业务字段。
     * 输入一条缺省记录和一条显式记录，预期仓储分别收到零及模拟来源、原始指数及来源，响应使用保存结果。
     */
    @Test
    void batchSaveAppliesDefaultsAndUsesPersistedResults() {
        LocalDateTime hour = LocalDateTime.of(2026, 1, 2, 8, 0);
        LocationHourlyFlowDTO missing = LocationHourlyFlowDTO.builder().locationId(1L)
                .recordHour(hour).visitorCount(0).dataSource(null).build();
        LocationHourlyFlowDTO explicit = LocationHourlyFlowDTO.builder().id(20L).locationId(2L)
                .recordHour(hour.plusHours(1)).visitorCount(320)
                .congestionIndex(new BigDecimal("1.25")).dataSource("sensor").build();
        when(repository.saveAll(anyList())).thenReturn(List.of(row(101L, 1L), row(102L, 2L)));

        List<LocationHourlyFlowDTO> result = service.batchSave(List.of(missing, explicit));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<LocationHourlyFlow>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
        LocationHourlyFlow first = captor.getValue().get(0);
        assertThat(first.getId()).isNull();
        assertThat(first.getLocationId()).isEqualTo(1L);
        assertThat(first.getRecordHour()).isEqualTo(hour);
        assertThat(first.getVisitorCount()).isZero();
        assertThat(first.getCongestionIndex()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(first.getDataSource()).isEqualTo("simulated");
        LocationHourlyFlow second = captor.getValue().get(1);
        assertThat(second.getId()).isEqualTo(20L);
        assertThat(second.getLocationId()).isEqualTo(2L);
        assertThat(second.getRecordHour()).isEqualTo(hour.plusHours(1));
        assertThat(second.getVisitorCount()).isEqualTo(320);
        assertThat(second.getCongestionIndex()).isEqualByComparingTo("1.25");
        assertThat(second.getDataSource()).isEqualTo("sensor");
        assertThat(missing.getCongestionIndex()).isNull();
        assertThat(missing.getDataSource()).isNull();
        assertThat(result).extracting(LocationHourlyFlowDTO::getId).containsExactly(101L, 102L);
    }

    /**
     * 空批次应作为空集合交给仓储且不产生记录。
     * 输入空列表和空保存结果，预期输出空列表。
     */
    @Test
    void batchSaveAcceptsEmptyBatch() {
        when(repository.saveAll(List.<LocationHourlyFlow>of())).thenReturn(List.of());
        assertThat(service.batchSave(List.of())).isEmpty();
        verify(repository).saveAll(List.<LocationHourlyFlow>of());
    }

    /**
     * 查询失败不能被包装成没有客流数据。
     * 输入抛出异常的历史仓储，预期原始异常向上传播。
     */
    @Test
    void findRecentPropagatesRepositoryFailure() {
        IllegalStateException failure = new IllegalStateException("历史查询失败");
        when(repository.findRecentAll(any())).thenThrow(failure);
        assertThatThrownBy(() -> service.findRecent(24, null)).isSameAs(failure);
    }

    /**
     * 最新状态读取异常不能伪装成空轮次。
     * 输入抛出异常的最新查询，预期保留原始异常。
     */
    @Test
    void findLatestPropagatesRepositoryFailure() {
        IllegalStateException failure = new IllegalStateException("最新查询失败");
        when(repository.findLatestForAllLocations()).thenThrow(failure);
        assertThatThrownBy(() -> service.findLatest(null)).isSameAs(failure);
    }

    /**
     * 批量保存失败必须向调用方暴露，不能返回未保存的输入数据。
     * 输入有效记录及写入异常，预期原异常传播。
     */
    @Test
    void batchSavePropagatesRepositoryFailure() {
        IllegalStateException failure = new IllegalStateException("保存失败");
        when(repository.saveAll(anyList())).thenThrow(failure);
        assertThatThrownBy(() -> service.batchSave(List.of(LocationHourlyFlowDTO.builder()
                .locationId(1L).visitorCount(1).recordHour(LocalDateTime.now()).build())))
                .isSameAs(failure);
    }

    private LocationHourlyFlow row(Long id, Long locationId) {
        return LocationHourlyFlow.builder().id(id).locationId(locationId)
                .recordHour(LocalDateTime.of(2026, 1, 2, 8, 0)).visitorCount(120)
                .congestionIndex(new BigDecimal("0.75")).dataSource("sensor").build();
    }
}
