package org.daiso.cidi.modules.spottrafficprediction.controller;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.daiso.cidi.core.error.CidiException;
import org.daiso.cidi.core.error.ErrorCode;
import org.daiso.cidi.modules.spottrafficprediction.entity.SpotTrafficPrediction;
import org.daiso.cidi.modules.spottrafficprediction.service.SpotTrafficPredictionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class SpotTrafficPredictionControllerTest {
    @Mock private SpotTrafficPredictionService service;
    @InjectMocks private SpotTrafficPredictionController controller;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(
                        JsonMapper.builder().addModule(new JavaTimeModule()).build())).build();
    }

    /**
     * 全量预测列表应按服务顺序输出。
     * 输入两条编号不递增的记录，预期响应保持原顺序而不自行排序。
     * @throws Exception 请求执行或响应断言失败时抛出
     */
    @Test
    void getAllPreservesServiceOrder() throws Exception {
        when(service.findAll()).thenReturn(List.of(SpotTrafficPrediction.builder().id(8L).build(),
                SpotTrafficPrediction.builder().id(3L).build()));
        mvc.perform(get("/api/spot-traffic-predictions")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(8)).andExpect(jsonPath("$[1].id").value(3));
        verify(service).findAll();
    }

    /**
     * 存在的预测记录应可通过编号查询。
     * 输入编号七及对应实体，预期响应成功且返回原实体。
     */
    @Test
    void getByIdReturnsExistingEntity() {
        SpotTrafficPrediction entity = SpotTrafficPrediction.builder().id(7L).build();
        when(service.findById(7L)).thenReturn(Optional.of(entity));
        var response = controller.getById(7L);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(entity);
    }

    /**
     * 查询、更新或删除不存在的预测必须报告资源不存在。
     * 输入不存在编号七，预期错误码和提示准确、不保存不删除且不修改请求编号。
     * @param operation 要验证的单条查询、更新或删除操作
     */
    @ParameterizedTest
    @ValueSource(strings = {"get", "update", "delete"})
    void missingRecordRaisesResourceNotFound(String operation) {
        when(service.findById(7L)).thenReturn(Optional.empty());
        SpotTrafficPrediction input = SpotTrafficPrediction.builder().id(99L).build();
        Throwable thrown = catchThrowable(() -> {
            switch (operation) {
                case "get" -> controller.getById(7L);
                case "update" -> controller.update(7L, input);
                case "delete" -> controller.delete(7L);
                default -> throw new IllegalArgumentException(operation);
            }
        });
        assertThat(thrown).isInstanceOf(CidiException.class).hasMessage("资源不存在: id=7");
        assertThat(((CidiException) thrown).getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
        assertThat(input.getId()).isEqualTo(99L);
        verify(service).findById(7L);
        verifyNoMoreInteractions(service);
    }

    /**
     * 新增预测应将输入实体交给服务，响应采用保存后实体。
     * 输入无编号预测与生成编号七的保存结果，预期返回保存结果且委托原输入。
     */
    @Test
    void createReturnsSavedEntity() {
        SpotTrafficPrediction input = new SpotTrafficPrediction();
        SpotTrafficPrediction saved = SpotTrafficPrediction.builder().id(7L).build();
        when(service.save(input)).thenReturn(saved);
        assertThat(controller.create(input)).isSameAs(saved);
        verify(service).save(same(input));
    }

    /**
     * 更新预测应以路径编号为准，但完整保留请求中的业务指标。
     * 输入冲突编号和各预测字段，预期仅编号替换为七，响应为服务保存后的结果。
     */
    @Test
    void updateUsesPathIdAndPreservesPredictionFields() {
        LocalDateTime predicted = LocalDateTime.of(2026, 1, 2, 9, 0);
        LocalDateTime created = predicted.minusHours(1);
        SpotTrafficPrediction input = SpotTrafficPrediction.builder().id(99L).spotId(2L)
                .predictTime(predicted).visitorCount(300).congestionLevel(2).trend("up")
                .trafficLevel(3).crowdDensity(new BigDecimal("1.25")).createTime(created).build();
        SpotTrafficPrediction saved = SpotTrafficPrediction.builder().id(7L).build();
        when(service.findById(7L)).thenReturn(Optional.of(new SpotTrafficPrediction()));
        when(service.save(same(input))).thenReturn(saved);
        var response = controller.update(7L, input);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(saved);
        assertThat(input.getId()).isEqualTo(7L);
        assertThat(input.getSpotId()).isEqualTo(2L);
        assertThat(input.getPredictTime()).isEqualTo(predicted);
        assertThat(input.getVisitorCount()).isEqualTo(300);
        assertThat(input.getCongestionLevel()).isEqualTo(2);
        assertThat(input.getTrend()).isEqualTo("up");
        assertThat(input.getTrafficLevel()).isEqualTo(3);
        assertThat(input.getCrowdDensity()).isEqualByComparingTo("1.25");
        assertThat(input.getCreateTime()).isEqualTo(created);
        var order = inOrder(service);
        order.verify(service).findById(7L);
        order.verify(service).save(same(input));
    }

    /**
     * 删除预测应先确认存在再执行删除，成功时不返回实体。
     * 输入存在编号七，预期先查询后删除并返回二百零四和空响应体。
     */
    @Test
    void deleteChecksExistenceBeforeDeleting() {
        when(service.findById(7L)).thenReturn(Optional.of(new SpotTrafficPrediction()));
        var response = controller.delete(7L);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
        var order = inOrder(service);
        order.verify(service).findById(7L);
        order.verify(service).deleteById(7L);
    }

    /**
     * 最新轮次路由应读取最新预测而非按编号查找。
     * 输入两条服务结果，预期响应保留景点、密度和记录顺序，且仅调用最新轮次方法。
     * @throws Exception 请求执行或响应断言失败时抛出
     */
    @Test
    void latestRouteReturnsLatestRound() throws Exception {
        when(service.findLatestRound()).thenReturn(List.of(
                SpotTrafficPrediction.builder().id(8L).spotId(1L).crowdDensity(new BigDecimal("2.25")).build(),
                SpotTrafficPrediction.builder().id(9L).spotId(2L).crowdDensity(new BigDecimal("0.25")).build()));
        mvc.perform(get("/api/spot-traffic-predictions/latest")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(8)).andExpect(jsonPath("$[0].spotId").value(1))
                .andExpect(jsonPath("$[0].crowdDensity").value(2.25)).andExpect(jsonPath("$[1].id").value(9));
        verify(service).findLatestRound();
        verifyNoMoreInteractions(service);
    }

    /**
     * 最近密度升序路由应使用独立的最近二十一条服务方法。
     * 输入已排序的预测结果，预期响应顺序不变且不访问全量或最新轮次查询。
     * @throws Exception 请求执行或响应断言失败时抛出
     */
    @Test
    void recentRoutePreservesSortedResults() throws Exception {
        when(service.findRecent21ByCrowdDensityAsc()).thenReturn(List.of(
                SpotTrafficPrediction.builder().id(2L).crowdDensity(BigDecimal.ZERO).build(),
                SpotTrafficPrediction.builder().id(1L).crowdDensity(new BigDecimal("2")).build()));
        mvc.perform(get("/api/spot-traffic-predictions/recent/crowd-density-asc"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[0].crowdDensity").value(0)).andExpect(jsonPath("$[1].id").value(1));
        verify(service).findRecent21ByCrowdDensityAsc();
        verifyNoMoreInteractions(service);
    }

    /**
     * 没有预测数据时各列表入口应返回空数组。
     * 输入全量、最新及最近查询的空结果，预期均为成功空响应。
     * @throws Exception 请求执行或响应断言失败时抛出
     */
    @Test
    void listRoutesHandleEmptyPredictions() throws Exception {
        when(service.findAll()).thenReturn(List.of());
        when(service.findLatestRound()).thenReturn(List.of());
        when(service.findRecent21ByCrowdDensityAsc()).thenReturn(List.of());
        mvc.perform(get("/api/spot-traffic-predictions")).andExpect(status().isOk()).andExpect(content().json("[]"));
        mvc.perform(get("/api/spot-traffic-predictions/latest"))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        mvc.perform(get("/api/spot-traffic-predictions/recent/crowd-density-asc"))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    /**
     * 批量预测应为缺失创建时间的记录补当前时间，并保留显式历史时间。
     * 输入缺省和显式时间的两条记录，预期分别补齐与保留时间，响应采用保存结果。
     */
    @Test
    void batchCreateFillsOnlyMissingCreationTimes() {
        LocalDateTime historical = LocalDateTime.of(2025, 1, 2, 8, 0);
        SpotTrafficPrediction missing = new SpotTrafficPrediction();
        SpotTrafficPrediction existing = SpotTrafficPrediction.builder().createTime(historical).build();
        List<SpotTrafficPrediction> input = List.of(missing, existing);
        List<SpotTrafficPrediction> saved = List.of(SpotTrafficPrediction.builder().id(88L).build());
        when(service.batchSave(same(input))).thenReturn(saved);
        LocalDateTime before = LocalDateTime.now();
        var response = controller.batchCreate(input);
        assertThat(missing.getCreateTime()).isBetween(before, LocalDateTime.now());
        assertThat(existing.getCreateTime()).isEqualTo(historical);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(saved);
        verify(service).batchSave(same(input));
    }

    /**
     * 批量预测请求应完整绑定各业务字段并输出保存后编号。
     * 输入预测时刻、人数、拥挤等级、趋势、交通等级和密度，预期服务收到全部原值及自动创建时间。
     * @throws Exception 请求执行或响应断言失败时抛出
     */
    @Test
    void batchRouteBindsAllPredictionFields() throws Exception {
        when(service.batchSave(anyList())).thenReturn(List.of(SpotTrafficPrediction.builder().id(88L).build()));
        mvc.perform(post("/api/spot-traffic-predictions/batch").contentType(MediaType.APPLICATION_JSON).content("""
                [{"spotId":2,"predictTime":"2026-01-02T08:00:00","visitorCount":0,
                  "congestionLevel":1,"trend":"stable","trafficLevel":2,"crowdDensity":0.25}]
                """))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(88));
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SpotTrafficPrediction>> captor = ArgumentCaptor.forClass(List.class);
        verify(service).batchSave(captor.capture());
        assertThat(captor.getValue()).singleElement().satisfies(entity -> {
            assertThat(entity.getSpotId()).isEqualTo(2L);
            assertThat(entity.getPredictTime()).isEqualTo(LocalDateTime.of(2026, 1, 2, 8, 0));
            assertThat(entity.getVisitorCount()).isZero();
            assertThat(entity.getCongestionLevel()).isEqualTo(1);
            assertThat(entity.getTrend()).isEqualTo("stable");
            assertThat(entity.getTrafficLevel()).isEqualTo(2);
            assertThat(entity.getCrowdDensity()).isEqualByComparingTo("0.25");
            assertThat(entity.getCreateTime()).isNotNull();
        });
    }

    /**
     * 空预测批次应合法委托服务且不生成任何记录。
     * 输入空数组，预期服务收到空列表并返回空数组。
     * @throws Exception 请求执行或响应断言失败时抛出
     */
    @Test
    void batchRouteAcceptsEmptyArray() throws Exception {
        when(service.batchSave(List.of())).thenReturn(List.of());
        mvc.perform(post("/api/spot-traffic-predictions/batch").contentType(MediaType.APPLICATION_JSON).content("[]"))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        verify(service).batchSave(List.of());
    }

    /**
     * 非法编号、缺失请求体及错误预测时间应在请求绑定阶段拒绝。
     * 输入无法解析的请求，预期四百状态且服务没有任何调用。
     * @throws Exception 请求执行或响应断言失败时抛出
     */
    @Test
    void rejectsMalformedRequestsBeforeServiceCall() throws Exception {
        mvc.perform(get("/api/spot-traffic-predictions/not-a-number")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/spot-traffic-predictions").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/spot-traffic-predictions/7").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/spot-traffic-predictions/batch").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/spot-traffic-predictions/batch").contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"predictTime\":\"not-a-time\"}]"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    /**
     * 各控制器入口不得吞掉服务失败并假装保存、删除或查询成功。
     * 输入所选服务方法抛出的固定异常，预期原异常向上传播。
     * @param operation 要验证的控制器操作名称
     */
    @ParameterizedTest
    @ValueSource(strings = {"all", "get", "create", "update", "delete", "batch", "latest", "recent"})
    void propagatesServiceFailures(String operation) {
        IllegalStateException failure = new IllegalStateException("服务失败");
        SpotTrafficPrediction input = new SpotTrafficPrediction();
        switch (operation) {
            case "all" -> {
                when(service.findAll()).thenThrow(failure);
                assertThatThrownBy(controller::getAll).isSameAs(failure);
            }
            case "get" -> {
                when(service.findById(7L)).thenThrow(failure);
                assertThatThrownBy(() -> controller.getById(7L)).isSameAs(failure);
            }
            case "create" -> {
                when(service.save(input)).thenThrow(failure);
                assertThatThrownBy(() -> controller.create(input)).isSameAs(failure);
            }
            case "update" -> {
                when(service.findById(7L)).thenReturn(Optional.of(new SpotTrafficPrediction()));
                when(service.save(same(input))).thenThrow(failure);
                assertThatThrownBy(() -> controller.update(7L, input)).isSameAs(failure);
            }
            case "delete" -> {
                when(service.findById(7L)).thenReturn(Optional.of(new SpotTrafficPrediction()));
                doThrow(failure).when(service).deleteById(7L);
                assertThatThrownBy(() -> controller.delete(7L)).isSameAs(failure);
            }
            case "batch" -> {
                when(service.batchSave(anyList())).thenThrow(failure);
                assertThatThrownBy(() -> controller.batchCreate(List.of(input))).isSameAs(failure);
            }
            case "latest" -> {
                when(service.findLatestRound()).thenThrow(failure);
                assertThatThrownBy(controller::getLatest).isSameAs(failure);
            }
            case "recent" -> {
                when(service.findRecent21ByCrowdDensityAsc()).thenThrow(failure);
                assertThatThrownBy(controller::getRecent21ByCrowdDensityAsc).isSameAs(failure);
            }
            default -> throw new IllegalArgumentException(operation);
        }
    }
}
