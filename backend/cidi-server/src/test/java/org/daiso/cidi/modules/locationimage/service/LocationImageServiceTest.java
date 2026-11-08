package org.daiso.cidi.modules.locationimage.service;

import java.util.List;
import java.util.Optional;
import org.daiso.cidi.modules.locationimage.entity.LocationImage;
import org.daiso.cidi.modules.locationimage.repository.LocationImageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocationImageServiceTest {
    private static final Long ID = 7L;
    @Mock private LocationImageRepository repository;
    private LocationImageService service;

    @BeforeEach
    void setUp() {
        service = new LocationImageService(repository);
    }

    /**
     * 列表查询应原样传递仓储数据，不过滤或重新排序。
     *
     * <p>测试输入：仓储返回两个不同实体组成的列表。</p>
     * <p>预期结果：返回同一列表实例，仓储仅查询一次。</p>
     */
    @Test
    void findAllPreservesResults() {
        List<LocationImage> rows = List.of(new LocationImage(), new LocationImage());
        when(repository.findAll()).thenReturn(rows);
        assertThat(service.findAll()).isSameAs(rows);
        verify(repository).findAll();
        verifyNoMoreInteractions(repository);
    }

    /**
     * 无记录时应返回空集合而不是制造数据。
     *
     * <p>测试输入：仓储返回空列表。</p>
     * <p>预期结果：结果为空，仓储查询一次。</p>
     */
    @Test
    void findAllReturnsEmptyList() {
        when(repository.findAll()).thenReturn(List.of());
        assertThat(service.findAll()).isEmpty();
        verify(repository).findAll();
    }

    /**
     * 按准确主键查找业务记录。
     *
     * <p>测试输入：主键 7 在仓储中存在。</p>
     * <p>预期结果：返回包含同一实体的 Optional，并传递原主键。</p>
     */
    @Test
    void findByIdReturnsExistingEntity() {
        LocationImage entity = new LocationImage();
        when(repository.findById(ID)).thenReturn(Optional.of(entity));
        assertThat(service.findById(ID)).containsSame(entity);
        verify(repository).findById(ID);
        verifyNoMoreInteractions(repository);
    }

    /**
     * Service 保留记录不存在的状态，交给调用层决定响应。
     *
     * <p>测试输入：主键 7 不存在，仓储返回 Optional.empty()。</p>
     * <p>预期结果：返回空 Optional，不调用保存或删除。</p>
     */
    @Test
    void findByIdPreservesAbsence() {
        when(repository.findById(ID)).thenReturn(Optional.empty());
        assertThat(service.findById(ID)).isEmpty();
        verify(repository).findById(ID);
        verifyNoMoreInteractions(repository);
    }

    /**
     * 保存应返回仓储处理后的实体，而非固定返回入参。
     *
     * <p>测试输入：一个待保存实体和仓储返回的另一已分配主键实体。</p>
     * <p>预期结果：原对象传入 save，返回仓储结果实例。</p>
     */
    @Test
    void saveReturnsRepositoryResult() {
        LocationImage input = new LocationImage();
        LocationImage persisted = new LocationImage();
        persisted.setId(ID);
        when(repository.save(input)).thenReturn(persisted);
        assertThat(service.save(input)).isSameAs(persisted);
        verify(repository).save(same(input));
        verifyNoMoreInteractions(repository);
    }

    /**
     * 删除操作必须以指定记录主键调用仓储。
     *
     * <p>测试输入：删除主键 7。</p>
     * <p>预期结果：deleteById(7) 恰调用一次，无额外仓储操作。</p>
     */
    @Test
    void deleteDelegatesExactIdentifier() {
        service.deleteById(ID);
        verify(repository).deleteById(ID);
        verifyNoMoreInteractions(repository);
    }

    /**
     * 持久化失败不得伪装成业务保存成功。
     *
     * <p>测试输入：仓储保存时抛出 IllegalStateException。</p>
     * <p>预期结果：异常原实例向上传递，保存只调用一次。</p>
     */
    @Test
    void savePropagatesRepositoryFailure() {
        LocationImage entity = new LocationImage();
        var failure = new IllegalStateException("save failed");
        when(repository.save(entity)).thenThrow(failure);
        assertThatThrownBy(() -> service.save(entity)).isSameAs(failure);
        verify(repository).save(entity);
    }

    /**
     * 删除失败不得被静默吞掉。
     *
     * <p>测试输入：仓储删除主键 7 时抛出 IllegalStateException。</p>
     * <p>预期结果：异常原实例向上传递。</p>
     */
    @Test
    void deletePropagatesRepositoryFailure() {
        var failure = new IllegalStateException("delete failed");
        doThrow(failure).when(repository).deleteById(ID);
        assertThatThrownBy(() -> service.deleteById(ID)).isSameAs(failure);
        verify(repository).deleteById(ID);
    }

    /**
     * 列表读取失败不能被替换成空列表。
     *
     * <p>测试输入：仓储 findAll 抛出 IllegalStateException。</p>
     * <p>预期结果：向上传递同一异常。</p>
     */
    @Test
    void findAllPropagatesRepositoryFailure() {
        var failure = new IllegalStateException("query failed");
        when(repository.findAll()).thenThrow(failure);
        assertThatThrownBy(() -> service.findAll()).isSameAs(failure);
        verify(repository).findAll();
    }

    /**
     * 查询故障与记录不存在必须保持区别。
     *
     * <p>测试输入：按主键查询时仓储抛出 IllegalStateException。</p>
     * <p>预期结果：向上传递同一异常，而非返回空 Optional。</p>
     */
    @Test
    void findByIdPropagatesRepositoryFailure() {
        var failure = new IllegalStateException("query failed");
        when(repository.findById(ID)).thenThrow(failure);
        assertThatThrownBy(() -> service.findById(ID)).isSameAs(failure);
        verify(repository).findById(ID);
    }

}
