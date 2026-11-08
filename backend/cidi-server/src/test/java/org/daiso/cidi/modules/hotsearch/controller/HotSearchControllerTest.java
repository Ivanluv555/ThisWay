package org.daiso.cidi.modules.hotsearch.controller;

import java.util.List;
import java.util.Optional;
import org.daiso.cidi.core.error.CidiException;
import org.daiso.cidi.core.error.ErrorCode;
import org.daiso.cidi.core.error.GlobalExceptionHandler;
import org.daiso.cidi.modules.hotsearch.entity.HotSearch;
import org.daiso.cidi.modules.hotsearch.service.HotSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class HotSearchControllerTest {
    private static final Integer ID = 7;
    private static final String ROUTE = "/api/hot-searches";
    @Mock private HotSearchService service;
    private HotSearchController controller;

    @BeforeEach
    void setUp() {
        controller = new HotSearchController(service);
    }

    private MockMvc mvc() {
        return MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    /**
     * 控制器将业务列表原样返回。
     *
     * <p>测试输入：Service 返回两个不同实体组成的列表。</p>
     * <p>预期结果：返回同一列表，Service 只查询一次。</p>
     */
    @Test
    void getAllReturnsServiceResults() {
        List<HotSearch> rows = List.of(new HotSearch(), new HotSearch());
        when(service.findAll()).thenReturn(rows);
        assertThat(controller.getAll()).isSameAs(rows);
        verify(service).findAll();
        verifyNoMoreInteractions(service);
    }

    /**
     * 存在的资源应使用成功状态返回。
     *
     * <p>测试输入：Service 返回主键 7 对应实体。</p>
     * <p>预期结果：HTTP 200，响应体为原实体。</p>
     */
    @Test
    void getByIdReturnsOk() {
        HotSearch entity = new HotSearch();
        when(service.findById(ID)).thenReturn(Optional.of(entity));
        var response = controller.getById(ID);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(entity);
        verify(service).findById(ID);
    }

    /**
     * 不存在的记录应报告明确业务错误。
     *
     * <p>测试输入：主键 7 查询为空。</p>
     * <p>预期结果：抛出 RESOURCE_NOT_FOUND，消息包含对应主键；无写操作。</p>
     */
    @Test
    void getByIdRejectsMissingResource() {
        when(service.findById(ID)).thenReturn(Optional.empty());
        assertMissing(() -> controller.getById(ID));
        verify(service).findById(ID);
        verifyNoMoreInteractions(service);
    }

    /**
     * 新增请求以保存结果为准，允许仓储补全主键。
     *
     * <p>测试输入：待保存对象与 Service 返回的另一实体。</p>
     * <p>预期结果：传入原对象，返回保存结果实例。</p>
     */
    @Test
    void createReturnsSavedEntity() {
        HotSearch input = new HotSearch();
        HotSearch saved = new HotSearch();
        saved.setId(ID);
        when(service.save(input)).thenReturn(saved);
        assertThat(controller.create(input)).isSameAs(saved);
        verify(service).save(same(input));
        verifyNoMoreInteractions(service);
    }

    /**
     * 更新必须以 URL 主键为准，不允许请求体主键改变更新目标。
     *
     * <p>测试输入：路径主键 7 存在，请求体主键为 99，Service 返回另一保存结果。</p>
     * <p>预期结果：保存前请求体主键被改为 7；先查询再保存；返回 HTTP 200 与保存结果。</p>
     */
    @Test
    void updateUsesPathIdentifier() {
        HotSearch input = new HotSearch();
        input.setId(99);
        HotSearch saved = new HotSearch();
        saved.setId(ID);
        when(service.findById(ID)).thenReturn(Optional.of(new HotSearch()));
        when(service.save(same(input))).thenAnswer(call -> {
            assertThat(input.getId()).isEqualTo(ID);
            return saved;
        });
        var response = controller.update(ID, input);
        assertThat(input.getId()).isEqualTo(ID);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(saved);
        var order = inOrder(service);
        order.verify(service).findById(ID);
        order.verify(service).save(same(input));
        verifyNoMoreInteractions(service);
    }

    /**
     * 不存在的记录不可通过更新操作隐式创建。
     *
     * <p>测试输入：路径主键 7 不存在，请求体主键为 99。</p>
     * <p>预期结果：抛出 RESOURCE_NOT_FOUND，不保存，输入主键仍为 99。</p>
     */
    @Test
    void updateMissingResourceDoesNotSave() {
        HotSearch input = new HotSearch();
        input.setId(99);
        when(service.findById(ID)).thenReturn(Optional.empty());
        assertMissing(() -> controller.update(ID, input));
        assertThat(input.getId()).isEqualTo(99);
        verify(service).findById(ID);
        verifyNoMoreInteractions(service);
    }

    /**
     * 删除前校验存在性，成功后不返回实体。
     *
     * <p>测试输入：主键 7 已存在。</p>
     * <p>预期结果：先查询再删除，响应为 HTTP 204，响应体为 null。</p>
     */
    @Test
    void deleteExistingResourceReturnsNoContent() {
        when(service.findById(ID)).thenReturn(Optional.of(new HotSearch()));
        var response = controller.delete(ID);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
        var order = inOrder(service);
        order.verify(service).findById(ID);
        order.verify(service).deleteById(ID);
        verifyNoMoreInteractions(service);
    }

    /**
     * 对不存在记录的删除请求应失败而非报告成功。
     *
     * <p>测试输入：主键 7 不存在。</p>
     * <p>预期结果：抛出 RESOURCE_NOT_FOUND，未调用 deleteById。</p>
     */
    @Test
    void deleteMissingResourceDoesNotDelete() {
        when(service.findById(ID)).thenReturn(Optional.empty());
        assertMissing(() -> controller.delete(ID));
        verify(service).findById(ID);
        verifyNoMoreInteractions(service);
    }

    /**
     * 控制器不能把保存异常转换为成功结果。
     *
     * <p>测试输入：Service.save 抛出 IllegalStateException。</p>
     * <p>预期结果：同一异常向上传递。</p>
     */
    @Test
    void createPropagatesSaveFailure() {
        HotSearch input = new HotSearch();
        var failure = new IllegalStateException("save failed");
        when(service.save(input)).thenThrow(failure);
        assertThatThrownBy(() -> controller.create(input)).isSameAs(failure);
    }

    /**
     * 存在性检查成功不代表删除一定成功。
     *
     * <p>测试输入：资源存在，但 Service.deleteById 抛出异常。</p>
     * <p>预期结果：向上传递原异常，不返回 204。</p>
     */
    @Test
    void deletePropagatesFailureAfterExistenceCheck() {
        when(service.findById(ID)).thenReturn(Optional.of(new HotSearch()));
        var failure = new IllegalStateException("delete failed");
        doThrow(failure).when(service).deleteById(ID);
        assertThatThrownBy(() -> controller.delete(ID)).isSameAs(failure);
        verify(service).deleteById(ID);
    }

    /**
     * 验证真实路由映射及空列表 JSON 响应，不启动完整应用。
     *
     * <p>测试输入：GET /api/hot-searches，Service 返回空集合。</p>
     * <p>预期结果：HTTP 200，响应体为 JSON 空数组。</p>
     *
     * @throws Exception 模拟 HTTP 请求执行或响应断言发生非预期错误时
     */
    @Test
    void httpListUsesMappedRoute() throws Exception {
        when(service.findAll()).thenReturn(List.of());
        mvc().perform(get(ROUTE)).andExpect(status().isOk()).andExpect(content().json("[]"));
        verify(service).findAll();
    }

    /**
     * 验证业务异常经统一处理器转换为 HTTP 错误响应。
     *
     * <p>测试输入：GET /api/hot-searches/7，资源不存在。</p>
     * <p>预期结果：HTTP 404，JSON code=REQ-404、status=404、path 为请求路径。</p>
     *
     * @throws Exception 模拟 HTTP 请求执行或响应断言发生非预期错误时
     */
    @Test
    void httpMissingResourceUsesErrorEnvelope() throws Exception {
        when(service.findById(ID)).thenReturn(Optional.empty());
        mvc().perform(get(ROUTE + "/7"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("REQ-404"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.path").value(ROUTE + "/7"));
    }

    /**
     * 验证 JSON 请求体被绑定为业务实体。
     *
     * <p>测试输入：POST /api/hot-searches，JSON 为 id=7。</p>
     * <p>预期结果：HTTP 200，返回 id=7，Service 收到主键为 7 的实体。</p>
     *
     * @throws Exception 模拟 HTTP 请求执行或响应断言发生非预期错误时
     */
    @Test
    void httpCreateBindsJsonBody() throws Exception {
        when(service.save(any(HotSearch.class))).thenAnswer(call -> call.getArgument(0));
        mvc().perform(post(ROUTE).contentType(MediaType.APPLICATION_JSON).content("{\"id\":7}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(7));
        verify(service).save(argThat(entity -> ID.equals(entity.getId())));
    }

    /**
     * 请求体无法解析时应在 HTTP 边界拒绝。
     *
     * <p>测试输入：POST /api/hot-searches，application/json 请求体为不完整 JSON。</p>
     * <p>预期结果：HTTP 400、REQ-400，Service 完全未调用。</p>
     *
     * @throws Exception 模拟 HTTP 请求执行或响应断言发生非预期错误时
     */
    @Test
    void httpMalformedJsonDoesNotReachService() throws Exception {
        mvc().perform(post(ROUTE).contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("REQ-400"));
        verifyNoInteractions(service);
    }

    /**
     * 验证路径参数和 JSON 请求体在 HTTP 层的组合绑定。
     *
     * <p>测试输入：PUT /api/hot-searches/7，请求体 id=99，记录 7 存在。</p>
     * <p>预期结果：HTTP 200，返回 id=7；保存参数主键为 7。</p>
     *
     * @throws Exception 模拟 HTTP 请求执行或响应断言发生非预期错误时
     */
    @Test
    void httpUpdateOverridesBodyId() throws Exception {
        when(service.findById(ID)).thenReturn(Optional.of(new HotSearch()));
        when(service.save(any(HotSearch.class))).thenAnswer(call -> call.getArgument(0));
        mvc().perform(put(ROUTE + "/7").contentType(MediaType.APPLICATION_JSON).content("{\"id\":99}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(7));
        verify(service).save(argThat(entity -> ID.equals(entity.getId())));
    }

    /**
     * 验证 DELETE 路由和无内容响应契约。
     *
     * <p>测试输入：DELETE /api/hot-searches/7，记录存在。</p>
     * <p>预期结果：HTTP 204，无响应体，删除调用一次。</p>
     *
     * @throws Exception 模拟 HTTP 请求执行或响应断言发生非预期错误时
     */
    @Test
    void httpDeleteReturnsEmptyBody() throws Exception {
        when(service.findById(ID)).thenReturn(Optional.of(new HotSearch()));
        mvc().perform(delete(ROUTE + "/7")).andExpect(status().isNoContent()).andExpect(content().string(""));
        verify(service).deleteById(ID);
    }

    /**
     * 更新记录时保存失败必须向上传递，不能返回虚假的成功响应。
     *
     * <p>测试输入：主键 7 已存在，Service.save 抛出 IllegalStateException。</p>
     * <p>预期结果：先查询后保存，抛出同一异常，输入实体的主键已按路径设为 7。</p>
     */
    @Test
    void updatePropagatesSaveFailure() {
        HotSearch input = new HotSearch();
        when(service.findById(ID)).thenReturn(Optional.of(new HotSearch()));
        var failure = new IllegalStateException("update failed");
        when(service.save(same(input))).thenThrow(failure);
        assertThatThrownBy(() -> controller.update(ID, input)).isSameAs(failure);
        assertThat(input.getId()).isEqualTo(ID);
        var order = inOrder(service);
        order.verify(service).findById(ID);
        order.verify(service).save(same(input));
        verifyNoMoreInteractions(service);
    }

    /**
     * 记录查询发生故障时，不应误报资源不存在或继续执行写操作。
     *
     * <p>测试输入：Service.findById(7) 抛出 IllegalStateException，分别调用查询、更新和删除。</p>
     * <p>预期结果：三次均抛出原异常，只有三次查询调用，没有保存或删除。</p>
     */
    @Test
    void lookupFailureStopsReadUpdateAndDelete() {
        var failure = new IllegalStateException("lookup failed");
        when(service.findById(ID)).thenThrow(failure);
        assertThatThrownBy(() -> controller.getById(ID)).isSameAs(failure);
        assertThatThrownBy(() -> controller.update(ID, new HotSearch())).isSameAs(failure);
        assertThatThrownBy(() -> controller.delete(ID)).isSameAs(failure);
        verify(service, times(3)).findById(ID);
        verifyNoMoreInteractions(service);
    }

    private void assertMissing(Runnable action) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(CidiException.class,
                ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND))
                .hasMessage("资源不存在: id=" + ID);
    }
}
