package org.daiso.cidi.modules.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.Optional;
import org.daiso.cidi.core.error.GlobalExceptionHandler;
import org.daiso.cidi.modules.user.entity.User;
import org.daiso.cidi.modules.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {
    @Mock private UserService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new UserController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    /**
     * 验证用户列表接口：输入两名用户的服务结果，预期 200 并保持返回顺序及用户资料。
     *
     * @throws Exception 当模拟 HTTP 请求处理失败时抛出
     */
    @Test
    void getAllReturnsUsersInOrder() throws Exception {
        when(service.findAll()).thenReturn(List.of(User.builder().id(2L).username("乙").build(),
                User.builder().id(1L).username("甲").build()));
        mvc.perform(get("/api/users")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(2)).andExpect(jsonPath("$[1].username").value("甲"));
        verify(service).findAll();
    }

    /**
     * 验证空用户列表仍是有效响应：输入空列表，预期 200 和 JSON 空数组。
     *
     * @throws Exception 当模拟 HTTP 请求处理失败时抛出
     */
    @Test
    void getAllReturnsEmptyArray() throws Exception {
        when(service.findAll()).thenReturn(List.of());
        mvc.perform(get("/api/users")).andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    /**
     * 验证按路径标识读取用户：输入存在的用户标识，预期 200 和对应用户实体。
     *
     * @throws Exception 当模拟 HTTP 请求处理失败时抛出
     */
    @Test
    void getByIdReturnsExistingUser() throws Exception {
        when(service.findById(7L)).thenReturn(Optional.of(User.builder().id(7L).username("小周").build()));
        mvc.perform(get("/api/users/7")).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7)).andExpect(jsonPath("$.username").value("小周"));
        verify(service).findById(7L);
    }

    /**
     * 验证创建用户请求的实体转换：输入注册资料 JSON，预期传递所有业务字段并返回已保存标识。
     *
     * @throws Exception 当模拟 HTTP 请求处理失败时抛出
     */
    @Test
    void createMapsUserAndReturnsSavedEntity() throws Exception {
        when(service.save(any())).thenReturn(User.builder().id(12L).username("小赵").build());
        mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"openid\":\"open\",\"username\":\"小赵\",\"avatarUrl\":\"a.png\","
                                + "\"bio\":\"简介\",\"phone\":\"13800000000\",\"status\":1}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(12));
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(service).save(captor.capture());
        assertThat(captor.getValue()).usingRecursiveComparison().isEqualTo(User.builder()
                .openid("open").username("小赵").avatarUrl("a.png").bio("简介").phone("13800000000").status(1).build());
    }

    /**
     * 验证更新以路径标识为准：输入路径 7 与正文 999，预期先检查 7 存在再以 7 保存正文资料。
     *
     * @throws Exception 当模拟 HTTP 请求处理失败时抛出
     */
    @Test
    void updateOverridesBodyIdWithPathId() throws Exception {
        when(service.findById(7L)).thenReturn(Optional.of(User.builder().id(7L).username("旧").build()));
        when(service.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        mvc.perform(put("/api/users/7").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":999,\"username\":\"新\",\"openid\":\"open\",\"status\":1}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.username").value("新"));
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        var order = inOrder(service);
        order.verify(service).findById(7L);
        order.verify(service).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(7L);
        assertThat(captor.getValue().getOpenid()).isEqualTo("open");
    }

    /**
     * 验证不存在用户的读改删统一拒绝：输入未登记标识，预期 404、资源错误码且不保存或删除。
     *
     * @param method 访问不存在资源的 HTTP 方法
     * @throws Exception 当模拟 HTTP 请求处理失败时抛出
     */
    @ParameterizedTest
    @ValueSource(strings = {"GET", "PUT", "DELETE"})
    void missingUserReturnsNotFoundWithoutMutation(String method) throws Exception {
        when(service.findById(99L)).thenReturn(Optional.empty());
        mvc.perform(request(HttpMethod.valueOf(method), "/api/users/99")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"新\"}"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("REQ-404"))
                .andExpect(jsonPath("$.message").value("资源不存在: id=99"));
        verify(service).findById(99L);
        verifyNoMoreInteractions(service);
    }

    /**
     * 验证存在用户可被删除：输入有效用户标识，预期先查后删且返回 204 无正文。
     *
     * @throws Exception 当模拟 HTTP 请求处理失败时抛出
     */
    @Test
    void deleteExistingUserReturnsNoContent() throws Exception {
        when(service.findById(7L)).thenReturn(Optional.of(User.builder().id(7L).build()));
        mvc.perform(delete("/api/users/7")).andExpect(status().isNoContent()).andExpect(content().string(""));
        var order = inOrder(service);
        order.verify(service).findById(7L);
        order.verify(service).deleteById(7L);
        order.verifyNoMoreInteractions();
    }

    /**
     * 验证用户写入的请求格式边界：输入损坏 JSON，预期 400 且服务未被调用。
     *
     * @param method 新增或更新使用的 HTTP 方法
     * @throws Exception 当模拟 HTTP 请求处理失败时抛出
     */
    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT"})
    void malformedWriteRequestIsRejected(String method) throws Exception {
        String path = "POST".equals(method) ? "/api/users" : "/api/users/7";
        mvc.perform(request(HttpMethod.valueOf(method), path).contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("REQ-400"));
        verifyNoInteractions(service);
    }

    /**
     * 验证服务异常不会泄露内部详情：输入保存失败，预期 500 标准错误且不返回底层异常文本。
     *
     * @throws Exception 当模拟 HTTP 请求处理失败时抛出
     */
    @Test
    void createReturnsServerErrorForUnexpectedFailure() throws Exception {
        when(service.save(any())).thenThrow(new IllegalStateException("internal database detail"));
        mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"新\"}"))
                .andExpect(status().isInternalServerError()).andExpect(jsonPath("$.code").value("SYS-500"))
                .andExpect(jsonPath("$.message").value("服务器内部错误"));
    }

    /**
     * 验证删除失败不会返回无内容成功：输入存在用户但删除服务失败，预期 500 标准错误。
     *
     * @throws Exception 当模拟 HTTP 请求处理失败时抛出
     */
    @Test
    void deleteFailureReturnsServerError() throws Exception {
        when(service.findById(7L)).thenReturn(Optional.of(User.builder().id(7L).build()));
        doThrow(new IllegalStateException("delete failed")).when(service).deleteById(7L);
        mvc.perform(delete("/api/users/7")).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("SYS-500"));
    }
}
