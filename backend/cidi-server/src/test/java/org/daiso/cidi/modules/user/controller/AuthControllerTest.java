package org.daiso.cidi.modules.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import org.daiso.cidi.core.error.CidiException;
import org.daiso.cidi.core.error.ErrorCode;
import org.daiso.cidi.core.error.GlobalExceptionHandler;
import org.daiso.cidi.modules.user.dto.*;
import org.daiso.cidi.modules.user.service.WechatAuthService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {
    @Mock private WechatAuthService service;
    private MockMvc mvc;
    private LocalValidatorFactoryBean validator;

    @BeforeEach
    void setUp() {
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new AuthController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).setValidator(validator).build();
    }

    @AfterEach
    void tearDown() {
        validator.close();
    }

    /**
     * 验证微信登录路由及 JSON 映射：输入完整登录资料，预期服务收到相同请求并返回令牌、环境和用户资料。
     *
     * @throws Exception 当模拟 HTTP 请求处理失败时抛出
     */
    @Test
    void loginMapsRequestAndReturnsResponse() throws Exception {
        var response = new WechatLoginResponse("token", LocalDateTime.of(2025, 1, 1, 12, 0), "trial",
                UserDTO.builder().id(3L).username("小林").phone("13800000000").build());
        when(service.login(any())).thenReturn(response);
        mvc.perform(post("/api/auth/wechat/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"wx-code\",\"nickname\":\"小林\",\"avatarUrl\":\"avatar.png\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token"))
                .andExpect(jsonPath("$.wechatEnv").value("trial"))
                .andExpect(jsonPath("$.expiresAt").exists())
                .andExpect(jsonPath("$.user.id").value(3))
                .andExpect(jsonPath("$.user.username").value("小林"))
                .andExpect(jsonPath("$.user.phone").value("13800000000"));
        ArgumentCaptor<WechatLoginRequest> captor = ArgumentCaptor.forClass(WechatLoginRequest.class);
        verify(service).login(captor.capture());
        assertThat(captor.getValue()).isEqualTo(new WechatLoginRequest("wx-code", "小林", "avatar.png"));
    }

    /**
     * 验证昵称头像为可选资料：输入仅含有效 code 的请求，预期登录成功并将可选字段以空引用传入服务。
     *
     * @throws Exception 当模拟 HTTP 请求处理失败时抛出
     */
    @Test
    void loginAllowsMissingOptionalProfile() throws Exception {
        when(service.login(any())).thenReturn(new WechatLoginResponse("token", null, "release", null));
        mvc.perform(post("/api/auth/wechat/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"code\"}"))
                .andExpect(status().isOk());
        verify(service).login(new WechatLoginRequest("code", null, null));
    }

    /**
     * 验证 HTTP 层拦截无效登录凭证：输入缺失、空或纯空白 code，预期 422 校验错误且不调用服务。
     *
     * @param body 无效登录请求 JSON
     * @throws Exception 当模拟 HTTP 请求处理失败时抛出
     */
    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"code\":null}", "{\"code\":\"\"}", "{\"code\":\"  \"}"})
    void loginRejectsInvalidCode(String body) throws Exception {
        mvc.perform(post("/api/auth/wechat/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("REQ-422"))
                .andExpect(jsonPath("$.message").value("code: code 不能为空"));
        verifyNoInteractions(service);
    }

    /**
     * 验证手机号绑定路由及响应字段：输入用户标识和取号凭证，预期服务参数完整并返回新绑定状态。
     *
     * @throws Exception 当模拟 HTTP 请求处理失败时抛出
     */
    @Test
    void bindPhoneMapsRequestAndResponse() throws Exception {
        when(service.bindPhone(any())).thenReturn(new WechatBindPhoneResponse(5L, "13900000000", true));
        mvc.perform(post("/api/auth/wechat/phone/bind").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":5,\"code\":\"phone-code\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(5))
                .andExpect(jsonPath("$.phone").value("13900000000"))
                .andExpect(jsonPath("$.newlyBound").value(true));
        verify(service).bindPhone(new WechatBindPhoneRequest(5L, "phone-code"));
    }

    /**
     * 验证手机号绑定请求的必填校验：输入缺失用户标识或无效 code，预期 422 且服务不执行。
     *
     * @param body 缺失必要参数的绑定 JSON
     * @throws Exception 当模拟 HTTP 请求处理失败时抛出
     */
    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"code\":\"valid\"}", "{\"userId\":null,\"code\":\"valid\"}",
            "{\"userId\":1}", "{\"userId\":1,\"code\":null}", "{\"userId\":1,\"code\":\"\"}",
            "{\"userId\":1,\"code\":\"  \"}"})
    void bindPhoneRejectsMissingRequiredValues(String body) throws Exception {
        mvc.perform(post("/api/auth/wechat/phone/bind").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("REQ-422"));
        verifyNoInteractions(service);
    }

    /**
     * 验证登录业务异常经真实异常处理器转换：输入被微信拒绝的凭证，预期 400、微信错误码及当前请求路径。
     *
     * @throws Exception 当模拟 HTTP 请求处理失败时抛出
     */
    @Test
    void loginReturnsWechatBusinessError() throws Exception {
        when(service.login(any())).thenThrow(new CidiException(ErrorCode.WECHAT_INVALID_CODE, "凭证已使用"));
        mvc.perform(post("/api/auth/wechat/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"used-code\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("WX-505"))
                .andExpect(jsonPath("$.message").value("凭证已使用"))
                .andExpect(jsonPath("$.path").value("/api/auth/wechat/login"));
    }

    /**
     * 验证手机号冲突不会返回成功：输入服务报告号码被占用的请求，预期 409 和手机号冲突错误码。
     *
     * @throws Exception 当模拟 HTTP 请求处理失败时抛出
     */
    @Test
    void bindPhoneReturnsConflict() throws Exception {
        when(service.bindPhone(any())).thenThrow(new CidiException(ErrorCode.PHONE_ALREADY_BOUND));
        mvc.perform(post("/api/auth/wechat/phone/bind").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":1,\"code\":\"code\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("USR-409"));
    }

    /**
     * 验证绑定不存在用户的 HTTP 契约：输入服务报告用户不存在的请求，预期 404 而不是空响应。
     *
     * @throws Exception 当模拟 HTTP 请求处理失败时抛出
     */
    @Test
    void bindPhoneReturnsNotFound() throws Exception {
        when(service.bindPhone(any())).thenThrow(new CidiException(ErrorCode.RESOURCE_NOT_FOUND));
        mvc.perform(post("/api/auth/wechat/phone/bind").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":99,\"code\":\"code\"}"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("REQ-404"));
    }

    /**
     * 验证外部微信故障的响应：输入服务抛出的微信接口异常，预期 502 而非登录成功。
     *
     * @throws Exception 当模拟 HTTP 请求处理失败时抛出
     */
    @Test
    void loginReturnsBadGatewayForWechatFailure() throws Exception {
        when(service.login(any())).thenThrow(new CidiException(ErrorCode.WECHAT_API_ERROR));
        mvc.perform(post("/api/auth/wechat/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"code\"}"))
                .andExpect(status().isBadGateway()).andExpect(jsonPath("$.code").value("WX-502"));
    }

    /**
     * 验证两个认证接口拒绝不可解析的请求：输入残缺 JSON，预期 400 且不进入认证服务。
     *
     * @param endpoint 认证接口路径
     * @throws Exception 当模拟 HTTP 请求处理失败时抛出
     */
    @ParameterizedTest
    @ValueSource(strings = {"/api/auth/wechat/login", "/api/auth/wechat/phone/bind"})
    void malformedJsonDoesNotReachService(String endpoint) throws Exception {
        mvc.perform(post(endpoint).contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("REQ-400"));
        verifyNoInteractions(service);
    }
}
