package org.daiso.cidi.modules.locationhourlyflow.controller;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.daiso.cidi.modules.locationhourlyflow.dto.LocationHourlyFlowDTO;
import org.daiso.cidi.modules.locationhourlyflow.service.LocationHourlyFlowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class LocationHourlyFlowControllerTest {
    @Mock private LocationHourlyFlowService service;
    @InjectMocks private LocationHourlyFlowController controller;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(
                        JsonMapper.builder().addModule(new JavaTimeModule()).build())).build();
    }

    /**
     * 未传历史筛选参数时应使用七天窗口且查询全部景点。
     * 输入不带参数的请求，预期服务收到一百六十八小时和空景点编号，响应为空数组。
     * @throws Exception 请求执行或响应断言失败时抛出
     */
    @Test
    void historyUsesSevenDayDefault() throws Exception {
        when(service.findRecent(168, null)).thenReturn(List.of());
        mvc.perform(get("/api/flow/history")).andExpect(status().isOk()).andExpect(content().json("[]"));
        verify(service).findRecent(168, null);
    }

    /**
     * 显式历史参数和所有响应业务字段应准确传递。
     * 输入二十四小时和景点编号，预期返回人数、拥挤指数、来源及指定格式的小时字符串。
     * @throws Exception 请求执行或响应断言失败时抛出
     */
    @Test
    void historyBindsFiltersAndSerializesPredictionHour() throws Exception {
        when(service.findRecent(24, 2L)).thenReturn(List.of(LocationHourlyFlowDTO.builder()
                .id(9L).locationId(2L).recordHour(LocalDateTime.of(2026, 1, 2, 8, 0))
                .visitorCount(120).congestionIndex(new BigDecimal("0.75")).dataSource("sensor").build()));
        mvc.perform(get("/api/flow/history").param("hours", "24").param("locationId", "2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(9))
                .andExpect(jsonPath("$[0].locationId").value(2))
                .andExpect(jsonPath("$[0].recordHour").value("2026-01-02T08:00:00"))
                .andExpect(jsonPath("$[0].visitorCount").value(120))
                .andExpect(jsonPath("$[0].congestionIndex").value(0.75))
                .andExpect(jsonPath("$[0].dataSource").value("sensor"));
        verify(service).findRecent(24, 2L);
    }

    /**
     * 无法解析的筛选参数应在请求绑定阶段被拒绝。
     * 输入非整数小时数和非法景点编号，预期各自返回四百状态且不调用业务服务。
     * @throws Exception 请求执行或响应断言失败时抛出
     */
    @Test
    void historyRejectsMalformedParameters() throws Exception {
        mvc.perform(get("/api/flow/history").param("hours", "abc")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/flow/history").param("locationId", "abc")).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    /**
     * 最新客流路由应分别支持全景点与单景点查询。
     * 输入无筛选请求和景点二的请求，预期分别委托空编号与指定编号并返回空数组。
     * @throws Exception 请求执行或响应断言失败时抛出
     */
    @Test
    void latestBindsOptionalLocation() throws Exception {
        when(service.findLatest(null)).thenReturn(List.of());
        when(service.findLatest(2L)).thenReturn(List.of());
        mvc.perform(get("/api/flow/history/latest")).andExpect(status().isOk()).andExpect(content().json("[]"));
        mvc.perform(get("/api/flow/history/latest").param("locationId", "2"))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        verify(service).findLatest(null);
        verify(service).findLatest(2L);
    }

    /**
     * 批量请求应按小时格式读取输入，并使用服务保存后的结果构造响应。
     * 输入未指定来源的单条客流，预期模型保留默认模拟来源且响应包含服务生成的编号。
     * @throws Exception 请求执行或响应断言失败时抛出
     */
    @Test
    void batchBindsFieldsAndReturnsSavedResult() throws Exception {
        when(service.batchSave(anyList())).thenReturn(List.of(LocationHourlyFlowDTO.builder().id(88L).build()));
        mvc.perform(post("/api/flow/history/batch").contentType(MediaType.APPLICATION_JSON).content("""
                [{"locationId":2,"recordHour":"2026-01-02T08:00:00","visitorCount":0,"congestionIndex":0.25}]
                """))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(88));
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<LocationHourlyFlowDTO>> captor = ArgumentCaptor.forClass(List.class);
        verify(service).batchSave(captor.capture());
        assertThat(captor.getValue()).singleElement().satisfies(dto -> {
            assertThat(dto.getLocationId()).isEqualTo(2L);
            assertThat(dto.getRecordHour()).isEqualTo(LocalDateTime.of(2026, 1, 2, 8, 0));
            assertThat(dto.getVisitorCount()).isZero();
            assertThat(dto.getCongestionIndex()).isEqualByComparingTo("0.25");
            assertThat(dto.getDataSource()).isEqualTo("simulated");
        });
    }

    /**
     * 空批次也是合法的批量输入，不应创建占位数据。
     * 输入空数组，预期服务收到空列表且响应为空数组。
     * @throws Exception 请求执行或响应断言失败时抛出
     */
    @Test
    void batchAcceptsEmptyArray() throws Exception {
        when(service.batchSave(List.of())).thenReturn(List.of());
        mvc.perform(post("/api/flow/history/batch").contentType(MediaType.APPLICATION_JSON).content("[]"))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        verify(service).batchSave(List.of());
    }

    /**
     * 缺失请求体或非法时间不得进入批量保存逻辑。
     * 输入无请求体及无法解析的记录小时，预期返回四百状态且服务无交互。
     * @throws Exception 请求执行或响应断言失败时抛出
     */
    @Test
    void batchRejectsMissingBodyAndMalformedHour() throws Exception {
        mvc.perform(post("/api/flow/history/batch").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/flow/history/batch").contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"recordHour\":\"not-a-time\"}]"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    /**
     * 控制器不得将历史、最新及写入失败伪装为成功响应。
     * 输入服务各方法抛出的同一异常，预期各控制器入口均保留该异常。
     */
    @Test
    void propagatesServiceFailures() {
        IllegalStateException failure = new IllegalStateException("服务失败");
        when(service.findRecent(24, 1L)).thenThrow(failure);
        when(service.findLatest(1L)).thenThrow(failure);
        when(service.batchSave(anyList())).thenThrow(failure);
        assertThatThrownBy(() -> controller.getHistory(24, 1L)).isSameAs(failure);
        assertThatThrownBy(() -> controller.getLatestFlows(1L)).isSameAs(failure);
        assertThatThrownBy(() -> controller.batchSave(List.of())).isSameAs(failure);
    }
}
