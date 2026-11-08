package org.daiso.cidi.modules.locationtransition.controller;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.daiso.cidi.core.error.CidiException;
import org.daiso.cidi.core.error.ErrorCode;
import org.daiso.cidi.modules.locationtransition.dto.NextDestinationDTO;
import org.daiso.cidi.modules.locationtransition.entity.LocationTransition;
import org.daiso.cidi.modules.locationtransition.service.LocationTransitionService;
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
class LocationTransitionControllerTest {
    @Mock private LocationTransitionService service;
    @InjectMocks private LocationTransitionController controller;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(
                        JsonMapper.builder().addModule(new JavaTimeModule()).build())).build();
    }

    /**
     * 全量转移查询应原样输出服务列表及其顺序。
     * 输入两条记录，预期全量路由依次输出两个编号。
     * @throws Exception 请求执行或响应断言失败时抛出
     */
    @Test
    void getAllReturnsRowsInServiceOrder() throws Exception {
        when(service.findAll()).thenReturn(List.of(LocationTransition.builder().id(8L).build(),
                LocationTransition.builder().id(3L).build()));
        mvc.perform(get("/api/location-transitions")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(8)).andExpect(jsonPath("$[1].id").value(3));
        verify(service).findAll();
    }

    /**
     * 单条查询存在时应返回成功状态和原实体。
     * 输入存在编号七，预期响应体为服务实体且状态为二百。
     */
    @Test
    void getByIdReturnsExistingEntity() {
        LocationTransition entity = LocationTransition.builder().id(7L).build();
        when(service.findById(7L)).thenReturn(Optional.of(entity));
        var response = controller.getById(7L);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(entity);
    }

    /**
     * 查找、更新和删除不存在的转移记录均应暴露资源不存在错误。
     * 输入不存在编号七，预期错误码和编号消息准确且不执行任何写入。
     * @param operation 要验证的单条查询、更新或删除操作
     */
    @ParameterizedTest
    @ValueSource(strings = {"get", "update", "delete"})
    void missingRecordRaisesResourceNotFound(String operation) {
        when(service.findById(7L)).thenReturn(Optional.empty());
        LocationTransition input = LocationTransition.builder().id(99L).build();
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
     * 新增记录应将原请求实体交给服务并返回保存后的实体。
     * 输入无编号实体与服务生成编号的结果，预期返回后者。
     */
    @Test
    void createReturnsSavedEntity() {
        LocationTransition input = new LocationTransition();
        LocationTransition saved = LocationTransition.builder().id(7L).build();
        when(service.save(input)).thenReturn(saved);
        assertThat(controller.create(input)).isSameAs(saved);
        verify(service).save(same(input));
    }

    /**
     * 更新时路径编号优先于请求体编号，其他预测字段必须保留。
     * 输入路径编号七、请求编号九十九及预测数据，预期保存编号七的输入实体并返回保存结果。
     */
    @Test
    void updateUsesPathIdAndKeepsBusinessFields() {
        LocationTransition input = LocationTransition.builder().id(99L).sourceLocationId(1L)
                .targetLocationId(2L).predictHour(LocalDateTime.of(2026, 1, 2, 8, 0))
                .transitionProbability(new BigDecimal("0.42")).expectedUsers(42)
                .couponAmount(new BigDecimal("5.50")).build();
        LocationTransition saved = LocationTransition.builder().id(7L).build();
        when(service.findById(7L)).thenReturn(Optional.of(new LocationTransition()));
        when(service.save(same(input))).thenReturn(saved);
        var response = controller.update(7L, input);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(saved);
        assertThat(input.getId()).isEqualTo(7L);
        assertThat(input.getSourceLocationId()).isEqualTo(1L);
        assertThat(input.getTargetLocationId()).isEqualTo(2L);
        assertThat(input.getPredictHour()).isEqualTo(LocalDateTime.of(2026, 1, 2, 8, 0));
        assertThat(input.getTransitionProbability()).isEqualByComparingTo("0.42");
        assertThat(input.getExpectedUsers()).isEqualTo(42);
        assertThat(input.getCouponAmount()).isEqualByComparingTo("5.50");
        var order = inOrder(service);
        order.verify(service).findById(7L);
        order.verify(service).save(same(input));
    }

    /**
     * 删除存在记录时应先检查再删除，并返回无内容状态。
     * 输入存在编号七，预期一次查询、一次删除且响应体为空。
     */
    @Test
    void deleteChecksExistenceAndReturnsNoContent() {
        when(service.findById(7L)).thenReturn(Optional.of(new LocationTransition()));
        var response = controller.delete(7L);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
        var order = inOrder(service);
        order.verify(service).findById(7L);
        order.verify(service).deleteById(7L);
    }

    /**
     * 下一目的地接口未指定窗口时默认六小时。
     * 输入来源景点二和含目的地的服务结果，预期服务收到六小时且响应保留比例、人数及时间。
     * @throws Exception 请求执行或响应断言失败时抛出
     */
    @Test
    void nextDestinationsUsesSixHourDefaultAndSerializesItems() throws Exception {
        NextDestinationDTO item = NextDestinationDTO.builder().hour("08:00")
                .transitions(List.of(NextDestinationDTO.TransitionItem.builder().locationId(3L)
                        .locationName("湖畔").ratio(0.42).ratioText("42%").users(42).build())).build();
        when(service.getNextDestinations(2L, 6)).thenReturn(List.of(item));
        mvc.perform(get("/api/location-transitions/next-destinations/2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].hour").value("08:00"))
                .andExpect(jsonPath("$[0].transitions[0].locationId").value(3))
                .andExpect(jsonPath("$[0].transitions[0].locationName").value("湖畔"))
                .andExpect(jsonPath("$[0].transitions[0].ratio").value(0.42))
                .andExpect(jsonPath("$[0].transitions[0].ratioText").value("42%"))
                .andExpect(jsonPath("$[0].transitions[0].users").value(42));
        verify(service).getNextDestinations(2L, 6);
    }

    /**
     * 显式预测小时数应替代默认值，包括零窗口边界。
     * 输入给定窗口且服务返回空列表，预期参数原样传递并返回空数组。
     * @param hours 显式指定的预测窗口小时数
     * @throws Exception 请求执行或响应断言失败时抛出
     */
    @ParameterizedTest
    @ValueSource(ints = {0, 2})
    void nextDestinationsBindsExplicitHours(int hours) throws Exception {
        when(service.getNextDestinations(2L, hours)).thenReturn(List.of());
        mvc.perform(get("/api/location-transitions/next-destinations/2").param("hours", String.valueOf(hours)))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        verify(service).getNextDestinations(2L, hours);
    }

    /**
     * 非法路径编号和非数字窗口应在绑定时被拒绝。
     * 输入不可解析的请求，预期四百状态且不调用任何服务方法。
     * @throws Exception 请求执行或响应断言失败时抛出
     */
    @Test
    void rejectsMalformedIdsAndHours() throws Exception {
        mvc.perform(get("/api/location-transitions/abc")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/location-transitions/next-destinations/2").param("hours", "abc"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    /**
     * 批量写入应仅填充缺失创建时间，不修改历史导入的时间。
     * 输入一条缺失时间和一条已有时间的记录，预期仅前者获得当前时间，响应使用服务保存结果。
     */
    @Test
    void batchCreateFillsOnlyMissingCreationTimes() {
        LocalDateTime historical = LocalDateTime.of(2025, 1, 2, 8, 0);
        LocationTransition missing = new LocationTransition();
        LocationTransition existing = LocationTransition.builder().createTime(historical).build();
        List<LocationTransition> input = List.of(missing, existing);
        List<LocationTransition> saved = List.of(LocationTransition.builder().id(88L).build());
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
     * 批量路由应绑定转移预测字段并返回保存编号。
     * 输入概率、人数、优惠金额和预测时间，预期服务收到完整业务值及自动创建时间。
     * @throws Exception 请求执行或响应断言失败时抛出
     */
    @Test
    void batchRouteBindsPredictionFields() throws Exception {
        when(service.batchSave(anyList())).thenReturn(List.of(LocationTransition.builder().id(88L).build()));
        mvc.perform(post("/api/location-transitions/batch").contentType(MediaType.APPLICATION_JSON).content("""
                [{"sourceLocationId":1,"targetLocationId":2,"predictHour":"2026-01-02T08:00:00",
                  "transitionProbability":0.42,"expectedUsers":42,"couponAmount":5.50}]
                """))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(88));
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<LocationTransition>> captor = ArgumentCaptor.forClass(List.class);
        verify(service).batchSave(captor.capture());
        assertThat(captor.getValue()).singleElement().satisfies(entity -> {
            assertThat(entity.getSourceLocationId()).isEqualTo(1L);
            assertThat(entity.getTargetLocationId()).isEqualTo(2L);
            assertThat(entity.getPredictHour()).isEqualTo(LocalDateTime.of(2026, 1, 2, 8, 0));
            assertThat(entity.getTransitionProbability()).isEqualByComparingTo("0.42");
            assertThat(entity.getExpectedUsers()).isEqualTo(42);
            assertThat(entity.getCouponAmount()).isEqualByComparingTo("5.50");
            assertThat(entity.getCreateTime()).isNotNull();
        });
    }

    /**
     * 空转移批次应正常返回空响应，不能添加预测记录。
     * 输入空列表，预期服务收到相同空列表且响应为空。
     */
    @Test
    void batchCreateAcceptsEmptyBatch() {
        List<LocationTransition> input = List.of();
        when(service.batchSave(input)).thenReturn(input);
        var response = controller.batchCreate(input);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
        verify(service).batchSave(input);
    }

    /**
     * 写入接口缺失请求体时不能进入业务服务。
     * 输入无请求体的新增、更新和批量请求，预期全部返回四百状态且没有服务交互。
     * @throws Exception 请求执行或响应断言失败时抛出
     */
    @Test
    void writeRoutesRejectMissingBodies() throws Exception {
        mvc.perform(post("/api/location-transitions").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/location-transitions/7").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/location-transitions/batch").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    /**
     * 服务查询或写入失败不能被控制器转换成成功结果。
     * 输入所选操作的服务异常，预期抛出原异常而不是返回成功响应。
     * @param operation 要验证的控制器操作名称
     */
    @ParameterizedTest
    @ValueSource(strings = {"all", "get", "create", "update", "delete", "batch", "next"})
    void propagatesServiceFailures(String operation) {
        IllegalStateException failure = new IllegalStateException("服务失败");
        LocationTransition input = new LocationTransition();
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
                when(service.findById(7L)).thenReturn(Optional.of(new LocationTransition()));
                when(service.save(same(input))).thenThrow(failure);
                assertThatThrownBy(() -> controller.update(7L, input)).isSameAs(failure);
            }
            case "delete" -> {
                when(service.findById(7L)).thenReturn(Optional.of(new LocationTransition()));
                doThrow(failure).when(service).deleteById(7L);
                assertThatThrownBy(() -> controller.delete(7L)).isSameAs(failure);
            }
            case "batch" -> {
                when(service.batchSave(anyList())).thenThrow(failure);
                assertThatThrownBy(() -> controller.batchCreate(List.of(input))).isSameAs(failure);
            }
            case "next" -> {
                when(service.getNextDestinations(1L, 6)).thenThrow(failure);
                assertThatThrownBy(() -> controller.getNextDestinations(1L, 6)).isSameAs(failure);
            }
            default -> throw new IllegalArgumentException(operation);
        }
    }
}
