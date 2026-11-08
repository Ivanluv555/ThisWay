package org.daiso.cidi.modules.usersetting.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.daiso.cidi.core.error.CidiException;
import org.daiso.cidi.core.error.ErrorCode;
import org.daiso.cidi.core.error.GlobalExceptionHandler;
import org.daiso.cidi.modules.usersetting.entity.UserSetting;
import org.daiso.cidi.modules.usersetting.service.UserSettingService;
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
class UserSettingControllerTest {
    @Mock private UserSettingService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new UserSettingController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    /**
     * 验证用户设置读取路径和序列化：输入用户标识 7，预期正确调用服务并返回全部偏好字段。
     *
     * @throws Exception 当模拟 HTTP 请求处理失败时抛出
     */
    @Test
    void getUserSettingReturnsPreferences() throws Exception {
        when(service.getUserSetting(7L)).thenReturn(UserSetting.builder().userId(7L).isPrivateAccount(1)
                .pushNotifications(0).themeColor("dark").build());
        mvc.perform(get("/api/user-settings/user/7")).andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(7)).andExpect(jsonPath("$.isPrivateAccount").value(1))
                .andExpect(jsonPath("$.pushNotifications").value(0)).andExpect(jsonPath("$.themeColor").value("dark"));
        verify(service).getUserSetting(7L);
    }

    /**
     * 验证更新接口把路径和正文分别传入服务：输入路径用户 7、正文用户 999 及设置，预期服务收到原正文和正确路径标识。
     *
     * @throws Exception 当模拟 HTTP 请求处理失败时抛出
     */
    @Test
    void updatePassesPathUserAndBodySeparately() throws Exception {
        when(service.updateUserSetting(eq(7L), any())).thenReturn(UserSetting.builder().userId(7L)
                .isPrivateAccount(0).pushNotifications(1).themeColor("light").build());
        mvc.perform(put("/api/user-settings/user/7").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":999,\"isPrivateAccount\":0,\"pushNotifications\":1,\"themeColor\":\"light\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.userId").value(7))
                .andExpect(jsonPath("$.isPrivateAccount").value(0)).andExpect(jsonPath("$.pushNotifications").value(1))
                .andExpect(jsonPath("$.themeColor").value("light"));
        ArgumentCaptor<UserSetting> captor = ArgumentCaptor.forClass(UserSetting.class);
        verify(service).updateUserSetting(eq(7L), captor.capture());
        assertThat(captor.getValue()).usingRecursiveComparison().isEqualTo(UserSetting.builder()
                .userId(999L).isPrivateAccount(0).pushNotifications(1).themeColor("light").build());
    }

    /**
     * 验证部分更新的未提供字段保持空值：输入仅主题的 JSON，预期服务可区分缺失的开关字段与显式零值。
     *
     * @throws Exception 当模拟 HTTP 请求处理失败时抛出
     */
    @Test
    void partialUpdateLeavesOmittedFieldsNull() throws Exception {
        when(service.updateUserSetting(eq(7L), any())).thenReturn(UserSetting.builder().userId(7L).themeColor("dark").build());
        mvc.perform(put("/api/user-settings/user/7").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"themeColor\":\"dark\"}"))
                .andExpect(status().isOk());
        ArgumentCaptor<UserSetting> captor = ArgumentCaptor.forClass(UserSetting.class);
        verify(service).updateUserSetting(eq(7L), captor.capture());
        assertThat(captor.getValue().getIsPrivateAccount()).isNull();
        assertThat(captor.getValue().getPushNotifications()).isNull();
        assertThat(captor.getValue().getThemeColor()).isEqualTo("dark");
    }

    /**
     * 验证空对象更新可到达服务的保留偏好分支：输入空 JSON 对象，预期 200 且服务收到无业务字段的更新。
     *
     * @throws Exception 当模拟 HTTP 请求处理失败时抛出
     */
    @Test
    void emptyObjectUpdateIsDelegated() throws Exception {
        when(service.updateUserSetting(eq(7L), any())).thenReturn(UserSetting.builder().userId(7L).build());
        mvc.perform(put("/api/user-settings/user/7").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
        ArgumentCaptor<UserSetting> captor = ArgumentCaptor.forClass(UserSetting.class);
        verify(service).updateUserSetting(eq(7L), captor.capture());
        assertThat(captor.getValue()).usingRecursiveComparison().isEqualTo(new UserSetting());
    }

    /**
     * 验证重置接口返回默认设置：输入重置请求，预期调用重置服务一次并返回默认开关及系统主题。
     *
     * @throws Exception 当模拟 HTTP 请求处理失败时抛出
     */
    @Test
    void resetReturnsDefaultPreferences() throws Exception {
        when(service.resetUserSetting(7L)).thenReturn(UserSetting.builder().userId(7L).isPrivateAccount(0)
                .pushNotifications(1).themeColor("system").build());
        mvc.perform(post("/api/user-settings/user/7/reset")).andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(7)).andExpect(jsonPath("$.isPrivateAccount").value(0))
                .andExpect(jsonPath("$.pushNotifications").value(1)).andExpect(jsonPath("$.themeColor").value("system"));
        verify(service).resetUserSetting(7L);
        verifyNoMoreInteractions(service);
    }

    /**
     * 验证更新请求体格式边界：输入空正文、JSON null 或损坏 JSON，预期 400 且不执行服务更新。
     *
     * @param body 缺失或格式错误的请求体
     * @throws Exception 当模拟 HTTP 请求处理失败时抛出
     */
    @ParameterizedTest
    @ValueSource(strings = {"", "null", "{"})
    void updateRejectsMissingOrMalformedBody(String body) throws Exception {
        mvc.perform(put("/api/user-settings/user/7").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("REQ-400"));
        verifyNoInteractions(service);
    }

    /**
     * 验证设置服务故障统一输出错误而非默认成功：输入读取、更新或重置异常，预期 500 且无内部细节泄露。
     *
     * @param operation 发生异常的设置操作
     * @throws Exception 当模拟 HTTP 请求处理失败时抛出
     */
    @ParameterizedTest
    @ValueSource(strings = {"get", "update", "reset"})
    void serviceFailureReturnsServerError(String operation) throws Exception {
        RuntimeException failure = new IllegalStateException("internal-storage-detail");
        String path = "/api/user-settings/user/7";
        HttpMethod method;
        if ("get".equals(operation)) {
            when(service.getUserSetting(7L)).thenThrow(failure);
            method = HttpMethod.GET;
        } else if ("update".equals(operation)) {
            when(service.updateUserSetting(eq(7L), any())).thenThrow(failure);
            method = HttpMethod.PUT;
        } else {
            when(service.resetUserSetting(7L)).thenThrow(failure);
            method = HttpMethod.POST;
            path += "/reset";
        }
        mvc.perform(request(method, path).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isInternalServerError()).andExpect(jsonPath("$.code").value("SYS-500"))
                .andExpect(jsonPath("$.message").value("服务器内部错误"));
    }

    /**
     * 验证设置控制器不吞掉业务异常：输入服务返回资源不存在错误，预期 404 并保留业务说明。
     *
     * @throws Exception 当模拟 HTTP 请求处理失败时抛出
     */
    @Test
    void getPreservesBusinessErrorContract() throws Exception {
        when(service.getUserSetting(7L)).thenThrow(new CidiException(ErrorCode.RESOURCE_NOT_FOUND, "用户不存在"));
        mvc.perform(get("/api/user-settings/user/7")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("REQ-404"))
                .andExpect(jsonPath("$.message").value("用户不存在"));
    }
}
