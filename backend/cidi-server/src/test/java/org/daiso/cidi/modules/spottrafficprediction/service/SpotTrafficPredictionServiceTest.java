package org.daiso.cidi.modules.spottrafficprediction.service;

import org.daiso.cidi.modules.spottrafficprediction.entity.SpotTrafficPrediction;
import org.daiso.cidi.modules.spottrafficprediction.repository.SpotTrafficPredictionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpotTrafficPredictionServiceTest {
    @Mock private SpotTrafficPredictionRepository repository;
    @InjectMocks private SpotTrafficPredictionService service;

    /**
     * 最近预测应先按创建时间取得第一页二十一条，再按人群密度升序排列。
     * 输入乱序密度、相等密度与空密度，预期页码零、容量二十一、同值稳定排序且空值排末尾。
     */
    @Test
    void recentRequestsFirstTwentyOneAndSortsDensityNullsLastStably() {
        List<SpotTrafficPrediction> rows = new ArrayList<>(List.of(
                row(1L, null), row(2L, "2.00"), row(3L, "0.00"),
                row(4L, "1.0"), row(5L, "1.00"), row(6L, null)));
        when(repository.findAllByOrderByCreateTimeDesc(any(Pageable.class))).thenReturn(rows);

        List<SpotTrafficPrediction> result = service.findRecent21ByCrowdDensityAsc();

        assertThat(result).extracting(SpotTrafficPrediction::getId).containsExactly(3L, 4L, 5L, 2L, 1L, 6L);
        assertThat(result.get(0).getCrowdDensity()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.get(4).getCrowdDensity()).isNull();
        ArgumentCaptor<Pageable> page = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAllByOrderByCreateTimeDesc(page.capture());
        assertThat(page.getValue().getPageNumber()).isZero();
        assertThat(page.getValue().getPageSize()).isEqualTo(21);
        assertThat(page.getValue().getOffset()).isZero();
        assertThat(page.getValue().getSort().isUnsorted()).isTrue();
        verifyNoMoreInteractions(repository);
    }

    /**
     * 二十一条最近记录应全部参与密度排序，不能遗漏分页上限处的记录。
     * 输入二十一条密度递减记录，预期输出数量仍为二十一且密度从零到二十递增。
     */
    @Test
    void recentSortsFullPageWithoutDroppingRows() {
        List<SpotTrafficPrediction> rows = new ArrayList<>();
        for (int i = 20; i >= 0; i--) {
            rows.add(row((long) i, Integer.toString(i)));
        }
        when(repository.findAllByOrderByCreateTimeDesc(any(Pageable.class))).thenReturn(rows);
        List<SpotTrafficPrediction> result = service.findRecent21ByCrowdDensityAsc();
        assertThat(result).hasSize(21);
        assertThat(result).extracting(SpotTrafficPrediction::getCrowdDensity).isSorted();
        assertThat(result.get(0).getId()).isZero();
        assertThat(result.get(20).getId()).isEqualTo(20L);
    }

    /**
     * 尚无最近预测时应返回空结果，不制造默认预测。
     * 输入空仓储列表，预期排序后仍为空。
     */
    @Test
    void recentHandlesEmptyPage() {
        when(repository.findAllByOrderByCreateTimeDesc(any(Pageable.class))).thenReturn(new ArrayList<>());
        assertThat(service.findRecent21ByCrowdDensityAsc()).isEmpty();
    }

    /**
     * 单条且密度未知的记录仍应保留。
     * 输入唯一空密度记录，预期返回同一实体且不补造密度。
     */
    @Test
    void recentHandlesSingleUnknownDensity() {
        SpotTrafficPrediction entity = row(1L, null);
        when(repository.findAllByOrderByCreateTimeDesc(any(Pageable.class)))
                .thenReturn(new ArrayList<>(List.of(entity)));
        assertThat(service.findRecent21ByCrowdDensityAsc()).containsExactly(entity);
        assertThat(entity.getCrowdDensity()).isNull();
    }

    /**
     * 全部密度未知时仍应保留仓储给出的创建时间顺序。
     * 输入三条空密度记录，预期排序稳定且记录完整保留。
     */
    @Test
    void recentPreservesOrderWhenEveryDensityIsNull() {
        when(repository.findAllByOrderByCreateTimeDesc(any(Pageable.class)))
                .thenReturn(new ArrayList<>(List.of(row(3L, null), row(1L, null), row(2L, null))));
        assertThat(service.findRecent21ByCrowdDensityAsc()).extracting(SpotTrafficPrediction::getId)
                .containsExactly(3L, 1L, 2L);
    }

    /**
     * 最新轮次查询应直接返回仓储选出的轮次，不与最近二十一条查询混用。
     * 输入按景点排序但密度逆序的最新记录，预期服务保持原顺序且不执行分页查询。
     */
    @Test
    void latestRoundPreservesRepositoryOrder() {
        List<SpotTrafficPrediction> rows = List.of(row(1L, "2.0"), row(2L, "1.0"));
        when(repository.findLatestRound()).thenReturn(rows);
        assertThat(service.findLatestRound()).isSameAs(rows);
        verify(repository).findLatestRound();
        verifyNoMoreInteractions(repository);
    }

    /**
     * 未产生任何预测轮次时应返回空列表。
     * 输入最新轮次空结果，预期不添加默认记录。
     */
    @Test
    void latestRoundHandlesNoPredictions() {
        when(repository.findLatestRound()).thenReturn(List.of());
        assertThat(service.findLatestRound()).isEmpty();
    }

    /**
     * 全量查询应保留仓储结果，不额外按密度排序。
     * 输入两条有序记录，预期直接返回原列表。
     */
    @Test
    void findAllReturnsRepositoryResult() {
        List<SpotTrafficPrediction> rows = List.of(row(2L, "2"), row(1L, "1"));
        when(repository.findAll()).thenReturn(rows);
        assertThat(service.findAll()).isSameAs(rows);
    }

    /**
     * 单条查询需要区分存在和不存在的预测。
     * 输入存在编号与不存在编号，预期分别返回原实体和空可选值。
     */
    @Test
    void findByIdPreservesPresentAndAbsentResults() {
        SpotTrafficPrediction entity = row(1L, "1");
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        when(repository.findById(9L)).thenReturn(Optional.empty());
        assertThat(service.findById(1L)).containsSame(entity);
        assertThat(service.findById(9L)).isEmpty();
    }

    /**
     * 保存单条预测应返回仓储持久化结果。
     * 输入无编号预测及带编号保存结果，预期原实体传入仓储而响应使用保存结果。
     */
    @Test
    void saveReturnsPersistedEntity() {
        SpotTrafficPrediction input = row(null, "1");
        SpotTrafficPrediction saved = row(8L, "1");
        when(repository.save(input)).thenReturn(saved);
        assertThat(service.save(input)).isSameAs(saved);
        verify(repository).save(same(input));
    }

    /**
     * 批量保存应委托原列表并返回仓储赋值后的预测。
     * 输入无编号记录列表及有编号的保存结果，预期返回后者。
     */
    @Test
    void batchSaveReturnsPersistedList() {
        List<SpotTrafficPrediction> input = List.of(row(null, "1"));
        List<SpotTrafficPrediction> saved = List.of(row(8L, "1"));
        when(repository.saveAll(input)).thenReturn(saved);
        assertThat(service.batchSave(input)).isSameAs(saved);
        verify(repository).saveAll(same(input));
    }

    /**
     * 空批次应保持为空，不能推导出默认预测记录。
     * 输入空列表，预期保存空集合并返回空集合。
     */
    @Test
    void batchSaveAcceptsEmptyList() {
        List<SpotTrafficPrediction> empty = List.of();
        when(repository.saveAll(empty)).thenReturn(empty);
        assertThat(service.batchSave(empty)).isEmpty();
        verify(repository).saveAll(empty);
    }

    /**
     * 删除操作应按指定编号精确委托仓储。
     * 输入编号七，预期只执行一次删除且不发起查询。
     */
    @Test
    void deleteDelegatesExactId() {
        service.deleteById(7L);
        verify(repository).deleteById(7L);
        verifyNoMoreInteractions(repository);
    }

    /**
     * 仓储错误必须传播，不能返回伪造的成功结果或空预测。
     * 输入所选仓储操作的固定异常，预期服务抛出相同异常实例。
     * @param operation 要验证的查询、写入、删除或预测操作
     */
    @ParameterizedTest
    @ValueSource(strings = {"all", "id", "save", "batch", "delete", "latest", "recent"})
    void propagatesRepositoryFailures(String operation) {
        IllegalStateException failure = new IllegalStateException("仓储失败");
        SpotTrafficPrediction entity = row(null, "1");
        switch (operation) {
            case "all" -> {
                when(repository.findAll()).thenThrow(failure);
                assertThatThrownBy(service::findAll).isSameAs(failure);
            }
            case "id" -> {
                when(repository.findById(1L)).thenThrow(failure);
                assertThatThrownBy(() -> service.findById(1L)).isSameAs(failure);
            }
            case "save" -> {
                when(repository.save(entity)).thenThrow(failure);
                assertThatThrownBy(() -> service.save(entity)).isSameAs(failure);
            }
            case "batch" -> {
                when(repository.saveAll(List.of(entity))).thenThrow(failure);
                assertThatThrownBy(() -> service.batchSave(List.of(entity))).isSameAs(failure);
            }
            case "delete" -> {
                doThrow(failure).when(repository).deleteById(1L);
                assertThatThrownBy(() -> service.deleteById(1L)).isSameAs(failure);
            }
            case "latest" -> {
                when(repository.findLatestRound()).thenThrow(failure);
                assertThatThrownBy(service::findLatestRound).isSameAs(failure);
            }
            case "recent" -> {
                when(repository.findAllByOrderByCreateTimeDesc(any(Pageable.class))).thenThrow(failure);
                assertThatThrownBy(service::findRecent21ByCrowdDensityAsc).isSameAs(failure);
            }
            default -> throw new IllegalArgumentException(operation);
        }
    }

    private SpotTrafficPrediction row(Long id, String density) {
        return SpotTrafficPrediction.builder().id(id).spotId(id)
                .crowdDensity(density == null ? null : new BigDecimal(density)).build();
    }
}
