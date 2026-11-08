package org.daiso.cidi.core.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import org.daiso.cidi.core.error.CidiException;
import org.daiso.cidi.core.error.ErrorCode;
import org.daiso.cidi.core.properties.Wxproperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.reactive.MockClientHttpRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

class WeChatApiUtilTest {

    private static final String TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/token"
            + "?grant_type=client_credential&appid=test-app&secret=test-secret";
    private static final String PHONE_URL = "https://wechat.invalid/phone";
    private Wxproperties properties;
    private RestTemplate restTemplate;
    private ExchangeFunction exchange;
    private WeChatApiUtil util;

    @BeforeEach
    void setUp() {
        properties = new Wxproperties();
        properties.setAppid("test-app");
        properties.setSecret("test-secret");
        properties.setPhoneApiUrl(PHONE_URL);
        restTemplate = mock(RestTemplate.class);
        exchange = mock(ExchangeFunction.class);
        // Every WebClient request terminates in the mock; no real HTTP connector is used.
        util = new WeChatApiUtil(properties, restTemplate, WebClient.builder().exchangeFunction(exchange));
    }

    /**
     * 验证登录会话解析及传入客户端的 URL 参数编码；RestTemplate 已被 mock，本用例不验证其内部二次编码行为。
     *
     * <p>测试输入：含空格、加号、斜杠、连接符号、问号与中文的登录 code；模拟返回 open-id、union-id、key 及 errcode=0。</p>
     * <p>预期结果：返回对应 WeChatSessionInfo；传给 RestTemplate 的 URL 含预期编码的 js_code，不调用手机号 WebClient。</p>
     */
    @Test
    void loginReturnsSessionAndEncodesCode() {
        loginResponse("{\"errcode\":0,\"openid\":\"open-id\",\"unionid\":\"union-id\",\"session_key\":\"key\"}");

        assertThat(util.getSessionInfoFromCode("code +/&?中文"))
                .isEqualTo(new WeChatApiUtil.WeChatSessionInfo("open-id", "union-id", "key"));
        verify(restTemplate).getForObject("https://api.weixin.qq.com/sns/jscode2session"
                + "?appid=test-app&secret=test-secret&js_code=code+%2B%2F%26%3F%E4%B8%AD%E6%96%87"
                + "&grant_type=authorization_code", String.class);
        verifyNoInteractions(exchange);
    }

    /**
     * openid 足以标识登录用户，可选字段及 errcode 缺失不阻止正常解析。
     *
     * <p>测试输入：code 为 code；模拟 JSON 仅包含 openid=open-id。</p>
     * <p>预期结果：返回 openid 为 open-id、unionid 与 sessionKey 均为 null 的会话。</p>
     */
    @Test
    void loginAllowsAbsentOptionalFieldsAndErrcode() {
        loginResponse("{\"openid\":\"open-id\"}");
        assertThat(util.getSessionInfoFromCode("code"))
                .isEqualTo(new WeChatApiUtil.WeChatSessionInfo("open-id", null, null));
    }

    /**
     * 输入校验优先于配置检查和 mock 回退，禁止使用空凭证生成模拟身份。
     *
     * <p>测试输入：code 分别为 Java null、空串、单空格、制表符加换行，共 4 组；appid 缺失且开启 mock 登录。</p>
     * <p>预期结果：每组均抛出 BAD_REQUEST 对应的 CidiException；两个 HTTP 客户端均无调用。</p>
     *
     * @param code 无效登录凭证，分别为 null、空字符串、空格、制表符加换行
     */
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n"})
    void loginRejectsBlankCodeBeforeConfigOrMockFallback(String code) {
        properties.setAppid(null);
        properties.setMockLoginEnabled(true);
        assertError(() -> util.getSessionInfoFromCode(code), ErrorCode.BAD_REQUEST);
        verifyNoInteractions(restTemplate, exchange);
    }

    /**
     * 未开启模拟登录时，微信认证必须具备非空应用凭据。
     *
     * <p>测试输入：分别以 Java null、空串、空格、制表符替代 appid，再恢复 appid 并以相同值替代 secret，共 4 组。</p>
     * <p>预期结果：各组的两次调用均抛出 WECHAT_CONFIG_MISSING，两个 HTTP 客户端均无调用。</p>
     *
     * @param missing 依次替代 appid 和 secret 的无效值：null、空字符串、空格或制表符
     */
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void loginRejectsMissingAppidOrSecret(String missing) {
        properties.setAppid(missing);
        assertError(() -> util.getSessionInfoFromCode("code"), ErrorCode.WECHAT_CONFIG_MISSING);
        properties.setAppid("test-app");
        properties.setSecret(missing);
        assertError(() -> util.getSessionInfoFromCode("code"), ErrorCode.WECHAT_CONFIG_MISSING);
        verifyNoInteractions(restTemplate, exchange);
    }

    /**
     * 区分凭证失效与上游接口故障，供调用方选择重新登录或故障处理。
     *
     * <p>测试输入：模拟登录响应 errcode 分别为 40029、40163、40013、-1，errmsg 均为 rejected。</p>
     * <p>预期结果：40029/40163 映射为 WECHAT_INVALID_CODE，40013/-1 映射为 WECHAT_API_ERROR；消息均为“微信 code2Session 失败: rejected”。</p>
     *
     * @param error 微信登录错误码：40029、40163、40013 或 -1
     * @param expected 对应的预期业务错误分类：凭证无效或接口调用失败
     */
    @ParameterizedTest
    @CsvSource({"40029,WECHAT_INVALID_CODE", "40163,WECHAT_INVALID_CODE", "40013,WECHAT_API_ERROR", "-1,WECHAT_API_ERROR"})
    void loginMapsWechatErrors(int error, ErrorCode expected) {
        loginResponse("{\"errcode\":" + error + ",\"errmsg\":\"rejected\"}");
        assertThatThrownBy(() -> util.getSessionInfoFromCode("code"))
                .isInstanceOfSatisfying(CidiException.class, ex -> assertThat(ex.getErrorCode()).isEqualTo(expected))
                .hasMessage("微信 code2Session 失败: rejected");
    }

    /**
     * 上游未提供可用身份标识时不能通过 mock 掩盖；此处 JSON 根 null 与字段 openid:null 的行为不同。
     *
     * <p>测试输入：开启 mock；响应分别为空串、JSON 根 null、空对象、openid 为空串、openid 为两个空格，共 5 组。</p>
     * <p>预期结果：每组均抛出 WECHAT_IDENTIFIER_MISSING，不返回模拟会话。</p>
     *
     * @param json 缺少可用 openid 的响应：空字符串、JSON 根 null、空对象或 openid 为空字符串/空白
     */
    @ParameterizedTest
    @ValueSource(strings = {"", "null", "{}", "{\"openid\":\"\"}", "{\"openid\":\"  \"}"})
    void loginMissingOpenidDoesNotFallBackEvenWithMockEnabled(String json) {
        properties.setMockLoginEnabled(true);
        loginResponse(json);
        assertError(() -> util.getSessionInfoFromCode("code"), ErrorCode.WECHAT_IDENTIFIER_MISSING);
    }

    /**
     * 已知缺陷记录：JSON null 被转换成非空文本而误获接受；正确业务预期应抛出 WECHAT_IDENTIFIER_MISSING，测试通过不代表缺陷已修复。
     *
     * <p>测试输入：开启 mock，模拟响应为字段 openid 显式等于 JSON null。</p>
     * <p>预期结果：当前实现返回 openid 为字符串 "null"、其余字段为 null 的会话；不调用手机号客户端。</p>
     */
    @Test
    void knownDefect_loginAcceptsJsonNullOpenidAsLiteralNullInsteadOfRejectingIt() {
        // Characterizes a defect, not the desired contract: explicit JSON null should
        // produce WECHAT_IDENTIFIER_MISSING, but asText() converts it to "null".
        properties.setMockLoginEnabled(true);
        loginResponse("{\"openid\":null}");
        assertThat(util.getSessionInfoFromCode("code"))
                .isEqualTo(new WeChatApiUtil.WeChatSessionInfo("null", null, null));
        verifyNoInteractions(exchange);
    }

    /**
     * 明确失效的登录凭证不能通过模拟登录回退绕过。
     *
     * <p>测试输入：开启 mock，模拟微信 errcode 分别为 40029、40163，共 2 组。</p>
     * <p>预期结果：每组均抛出 WECHAT_INVALID_CODE，不返回模拟会话。</p>
     *
     * @param error 表示登录凭证无效的微信错误码，分别为 40029 和 40163
     */
    @ParameterizedTest
    @ValueSource(ints = {40029, 40163})
    void mockDoesNotHideInvalidLoginCode(int error) {
        properties.setMockLoginEnabled(true);
        loginResponse("{\"errcode\":" + error + "}");
        assertError(() -> util.getSessionInfoFromCode("code"), ErrorCode.WECHAT_INVALID_CODE);
    }

    /**
     * 将网络传输失败统一封装为业务异常，同时保留诊断原因。
     *
     * <p>测试输入：登录 code 为 code，模拟 RestTemplate 抛出 ResourceAccessException("offline")。</p>
     * <p>预期结果：抛出 WECHAT_API_ERROR，消息为“调用微信接口异常”，cause 保留原异常。</p>
     */
    @Test
    void loginWrapsTransportFailureWithCause() {
        var cause = new ResourceAccessException("offline");
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenThrow(cause);
        assertThatThrownBy(() -> util.getSessionInfoFromCode("code"))
                .isInstanceOfSatisfying(CidiException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.WECHAT_API_ERROR))
                .hasMessage("调用微信接口异常").hasCause(cause);
    }

    /**
     * 上游响应无法读取或解析时按接口异常处理，而非成功登录。
     *
     * <p>测试输入：模拟响应分别为 Java null 和非法 JSON 字符串 not-json，共 2 组。</p>
     * <p>预期结果：每组均抛出 WECHAT_API_ERROR 对应的 CidiException。</p>
     *
     * @param json 不可解析的模拟登录响应，分别为 Java null 和 not-json
     */
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"not-json"})
    void loginWrapsUnusableResponse(String json) {
        loginResponse(json);
        assertError(() -> util.getSessionInfoFromCode("code"), ErrorCode.WECHAT_API_ERROR);
    }

    /**
     * 确认模拟身份会去空白、转小写、过滤非字母数字；过滤后为空使用匿名后缀，超长后缀截取 20 字符。
     *
     * <p>测试输入：appid 缺失且开启 mock；三组 code 分别为带首尾空格的 Ab-C_12!、中文!?、ABCDEFGHIJKLMNOPQRSTUVWXYZ。</p>
     * <p>预期结果：分别得到规范化后缀 abc12、anonymous、abcdefghijklmnopqrst；openid 为 mock_ 加后缀，sessionKey 为 mock_session_ 加后缀，unionid 为 null；不调用 HTTP 客户端。</p>
     *
     * @param code 待规范化的登录凭证，覆盖混合字符、无有效字符及超长字符
     * @param normalized 期望的规范化后缀，分别为 abc12、anonymous 和 abcdefghijklmnopqrst
     */
    @ParameterizedTest
    @CsvSource({"' Ab-C_12! ',abc12", "'中文!?',anonymous", "ABCDEFGHIJKLMNOPQRSTUVWXYZ,abcdefghijklmnopqrst"})
    void mockLoginNormalizesCodeWhenConfigMissing(String code, String normalized) {
        properties.setAppid(null);
        properties.setMockLoginEnabled(true);
        assertMockSession(code, normalized);
        verifyNoInteractions(restTemplate, exchange);
    }

    /**
     * 模拟模式允许在上游接口或响应解析故障时继续本地登录流程。
     *
     * <p>测试输入：开启 mock，code 为 Test-Code；模拟响应分别为 Java null、errcode=-1 的 JSON、not-json，共 3 组。</p>
     * <p>预期结果：均返回 openid=mock_testcode、sessionKey=mock_session_testcode、unionid=null。</p>
     *
     * @param json 触发回退的模拟响应：Java null、errcode=-1 的 JSON 或 not-json
     */
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"{\"errcode\":-1}", "not-json"})
    void mockLoginFallsBackOnApiAndParsingFailures(String json) {
        properties.setMockLoginEnabled(true);
        loginResponse(json);
        assertMockSession("Test-Code", "testcode");
    }

    /**
     * 确认模拟模式下网络不可用也能触发预定的本地回退。
     *
     * <p>测试输入：开启 mock，code 为 Test-Code，模拟网络调用抛出 ResourceAccessException。</p>
     * <p>预期结果：返回 mock_testcode 与 mock_session_testcode，unionid 为 null。</p>
     */
    @Test
    void mockLoginFallsBackOnTransportFailure() {
        properties.setMockLoginEnabled(true);
        when(restTemplate.getForObject(anyString(), eq(String.class)))
                .thenThrow(new ResourceAccessException("offline"));
        assertMockSession("Test-Code", "testcode");
    }

    /**
     * 模拟开关仅允许失败回退，不替换成功获取的真实身份。
     *
     * <p>测试输入：开启 mock，但模拟上游成功返回 openid=real-id。</p>
     * <p>预期结果：返回真实响应中的 real-id，而非 mock 前缀标识。</p>
     */
    @Test
    void mockFlagDoesNotReplaceSuccessfulRealLogin() {
        properties.setMockLoginEnabled(true);
        loginResponse("{\"openid\":\"real-id\"}");
        assertThat(util.getSessionInfoFromCode("code").openid()).isEqualTo("real-id");
    }

    /**
     * 优先使用纯手机号，并验证请求方法、认证参数以及反斜杠和引号的 JSON 转义。
     *
     * <p>测试输入：有效 token；包含反斜杠和双引号的手机号 code；响应同时包含纯号码 13800138000 和完整号码 +8613800138000。</p>
     * <p>预期结果：返回纯号码；请求为带 access_token=token 的 POST，Content-Type 为 application/json，序列化请求体再解析得到原 code；token URL 被调用一次。</p>
     *
     * @throws Exception 解析捕获的 JSON 请求体发生非预期错误时
     */
    @Test
    void phonePrefersPureNumberAndSendsTokenAndEscapedJson() throws Exception {
        validToken();
        phoneResponse("{\"errcode\":0,\"phone_info\":{\"purePhoneNumber\":\"13800138000\",\"phoneNumber\":\"+8613800138000\"}}");
        String code = "phone\\code\"quoted";
        assertThat(util.getPhoneNumberByCode(code)).isEqualTo("13800138000");

        var captor = ArgumentCaptor.forClass(ClientRequest.class);
        verify(exchange).exchange(captor.capture());
        ClientRequest request = captor.getValue();
        assertThat(request.method()).isEqualTo(HttpMethod.POST);
        assertThat(request.url().toString()).isEqualTo(PHONE_URL + "?access_token=token");
        assertThat(request.headers().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        var output = new MockClientHttpRequest(HttpMethod.POST, URI.create(PHONE_URL));
        request.writeTo(output, ExchangeStrategies.withDefaults()).block();
        assertThat(new ObjectMapper().readTree(output.getBodyAsString().block()).path("code").asText())
                .isEqualTo(code);
        verify(restTemplate).getForObject(TOKEN_URL, String.class);
    }

    /**
     * 已知缺陷记录：手动编码叠加 WebClient 编码导致 token 被改写；正确业务预期是服务端单次解码即可恢复原值，mock 成功不代表真实请求能成功。
     *
     * <p>测试输入：token 为 token+/=&，模拟手机号接口成功返回 13800138000。</p>
     * <p>预期结果：返回模拟手机号；捕获到原始查询串 access_token=token%252B%252F%253D%2526，解码一次仍不等于原 token。</p>
     */
    @Test
    void knownDefect_phoneDoubleEncodesSpecialCharactersInAccessToken() {
        // Characterizes a defect: URLEncoder followed by WebClient's URI-template
        // encoding changes percent escapes again, so the server receives the wrong token.
        tokenResponse("{\"access_token\":\"token+/=&\",\"expires_in\":7200}");
        phoneResponse("{\"phone_info\":{\"purePhoneNumber\":\"13800138000\"}}");
        assertThat(util.getPhoneNumberByCode("code")).isEqualTo("13800138000");
        var captor = ArgumentCaptor.forClass(ClientRequest.class);
        verify(exchange).exchange(captor.capture());
        assertThat(captor.getValue().url().getRawQuery())
                .isEqualTo("access_token=token%252B%252F%253D%2526");
        assertThat(captor.getValue().url().getQuery())
                .isEqualTo("access_token=token%2B%2F%3D%26")
                .isNotEqualTo("access_token=token+/=&");
    }

    /**
     * 纯号码不可用时允许回退到上游提供的完整号码。
     *
     * <p>测试输入：有效 token；两组响应分别缺少 purePhoneNumber 或将其设为空白，phoneNumber 均为 +8613800138000。</p>
     * <p>预期结果：两组均返回完整号码 +8613800138000。</p>
     *
     * @param json 纯号码缺失或为空白、完整号码为 +8613800138000 的响应
     */
    @ParameterizedTest
    @ValueSource(strings = {"{\"phone_info\":{\"phoneNumber\":\"+8613800138000\"}}",
            "{\"phone_info\":{\"purePhoneNumber\":\"  \",\"phoneNumber\":\"+8613800138000\"}}"})
    void phoneFallsBackToFullNumber(String json) {
        validToken();
        phoneResponse(json);
        assertThat(util.getPhoneNumberByCode("code")).isEqualTo("+8613800138000");
    }

    /**
     * 缺少有效号码时明确失败，避免把无号码响应当作绑定成功。
     *
     * <p>测试输入：有效 token；响应分别为 JSON 根 null、空对象、phone_info:null、空 phone_info、纯号码为空且完整号码为空白，共 5 组。</p>
     * <p>预期结果：每组均抛出 WECHAT_PHONE_NUMBER_MISSING。</p>
     *
     * @param json 缺少有效号码的响应，覆盖根 null、字段缺失、空对象及空白号码
     */
    @ParameterizedTest
    @ValueSource(strings = {"null", "{}", "{\"phone_info\":null}", "{\"phone_info\":{}}",
            "{\"phone_info\":{\"purePhoneNumber\":\"\",\"phoneNumber\":\"  \"}}"})
    void phoneRejectsMissingNumber(String json) {
        validToken();
        phoneResponse(json);
        assertError(() -> util.getPhoneNumberByCode("code"), ErrorCode.WECHAT_PHONE_NUMBER_MISSING);
    }

    /**
     * 手机号获取不使用登录 mock 回退，错误必须如实传递给调用方。
     *
     * <p>测试输入：开启 mock 且 token 有效；手机号响应 errcode 为 40029、40163、40001，errmsg=rejected。</p>
     * <p>预期结果：40029/40163 映射 WECHAT_INVALID_CODE，40001 映射 WECHAT_API_ERROR；消息为“微信 getPhoneNumber 失败: rejected”。</p>
     *
     * @param error 微信手机号接口错误码：40029、40163 或 40001
     * @param expected 对应的预期业务错误分类：凭证无效或接口调用失败
     */
    @ParameterizedTest
    @CsvSource({"40029,WECHAT_INVALID_CODE", "40163,WECHAT_INVALID_CODE", "40001,WECHAT_API_ERROR"})
    void phoneMapsWechatErrorsWithoutMockFallback(int error, ErrorCode expected) {
        properties.setMockLoginEnabled(true);
        validToken();
        phoneResponse("{\"errcode\":" + error + ",\"errmsg\":\"rejected\"}");
        assertThatThrownBy(() -> util.getPhoneNumberByCode("code"))
                .isInstanceOfSatisfying(CidiException.class, ex -> assertThat(ex.getErrorCode()).isEqualTo(expected))
                .hasMessage("微信 getPhoneNumber 失败: rejected");
    }

    /**
     * 在产生外部请求前拒绝空授权凭证。
     *
     * <p>测试输入：凭据配置有效；手机号 code 分别为 Java null、空串、空格、制表符加换行，共 4 组。</p>
     * <p>预期结果：每组均抛出 BAD_REQUEST，既不请求 token，也不调用手机号接口。</p>
     *
     * @param code 无效手机号授权凭证，分别为 null、空字符串、空格、制表符加换行
     */
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n"})
    void phoneRejectsBlankCode(String code) {
        assertError(() -> util.getPhoneNumberByCode(code), ErrorCode.BAD_REQUEST);
        verifyNoInteractions(restTemplate, exchange);
    }

    /**
     * 手机号接口必须使用有效应用配置，不能依赖模拟登录开关放宽要求。
     *
     * <p>测试输入：开启 mock；分别用 Java null、空串、空格替代 appid，再恢复 appid 并用相同值替代 secret，共 3 组。</p>
     * <p>预期结果：各组的两次调用均抛出 WECHAT_CONFIG_MISSING，两个 HTTP 客户端均无调用。</p>
     *
     * @param missing 依次替代 appid 和 secret 的无效值：null、空字符串或空格
     */
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void phoneRequiresCredentialsEvenWithMockEnabled(String missing) {
        properties.setMockLoginEnabled(true);
        properties.setAppid(missing);
        assertError(() -> util.getPhoneNumberByCode("code"), ErrorCode.WECHAT_CONFIG_MISSING);
        properties.setAppid("test-app");
        properties.setSecret(missing);
        assertError(() -> util.getPhoneNumberByCode("code"), ErrorCode.WECHAT_CONFIG_MISSING);
        verifyNoInteractions(restTemplate, exchange);
    }

    /**
     * 缺少目标接口地址时禁止发送手机号请求；当前流程在此检查前已获取 token。
     *
     * <p>测试输入：token 响应有效；手机号接口地址分别为 Java null、空串、空格，共 3 组。</p>
     * <p>预期结果：每组均抛出 WECHAT_CONFIG_MISSING，手机号 WebClient 无调用。</p>
     *
     * @param endpoint 未配置的手机号接口地址，分别为 null、空字符串和空格
     */
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void phoneRequiresEndpoint(String endpoint) {
        validToken();
        properties.setPhoneApiUrl(endpoint);
        assertError(() -> util.getPhoneNumberByCode("code"), ErrorCode.WECHAT_CONFIG_MISSING);
        verifyNoInteractions(exchange);
    }

    /**
     * 无法解析的手机号响应统一视为上游接口故障。
     *
     * <p>测试输入：token 有效，手机号接口返回 not-json。</p>
     * <p>预期结果：抛出 WECHAT_API_ERROR。</p>
     */
    @Test
    void phoneWrapsMalformedJsonResponse() {
        validToken();
        phoneResponse("not-json");
        assertError(() -> util.getPhoneNumberByCode("code"), ErrorCode.WECHAT_API_ERROR);
    }

    /**
     * 记录当前解析行为：空字符串形成缺失节点，随后按号码缺失处理。
     *
     * <p>测试输入：token 有效，模拟手机号响应体为空字符串。</p>
     * <p>预期结果：抛出 WECHAT_PHONE_NUMBER_MISSING，而不是 WECHAT_API_ERROR。</p>
     */
    @Test
    void phoneTreatsEmptyStringResponseAsMissingNumber() {
        validToken();
        // An empty string parses to MissingNode, not a JSON parsing exception.
        phoneResponse("");
        assertError(() -> util.getPhoneNumberByCode("code"), ErrorCode.WECHAT_PHONE_NUMBER_MISSING);
    }

    /**
     * 响应式传输错误在同步调用边界转换为统一业务异常，保留排查线索。
     *
     * <p>测试输入：token 有效，手机号 ExchangeFunction 返回包含 IllegalStateException("offline") 的错误信号。</p>
     * <p>预期结果：抛出 WECHAT_API_ERROR，消息为“调用微信手机号接口异常”，cause 保留原异常。</p>
     */
    @Test
    void phoneWrapsTransportFailureWithCause() {
        validToken();
        var cause = new IllegalStateException("offline");
        when(exchange.exchange(any())).thenReturn(Mono.error(cause));
        assertThatThrownBy(() -> util.getPhoneNumberByCode("code"))
                .isInstanceOfSatisfying(CidiException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.WECHAT_API_ERROR))
                .hasMessage("调用微信手机号接口异常").hasCause(cause);
    }

    /**
     * 上游 HTTP 失败状态不能进入正常手机号解析成功路径。
     *
     * <p>测试输入：token 有效，手机号接口模拟返回 HTTP 500 和响应体 server failed。</p>
     * <p>预期结果：抛出 WECHAT_API_ERROR。</p>
     */
    @Test
    void phoneWrapsHttpError() {
        validToken();
        when(exchange.exchange(any())).thenReturn(Mono.just(ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("server failed").build()));
        assertError(() -> util.getPhoneNumberByCode("code"), ErrorCode.WECHAT_API_ERROR);
    }

    /**
     * 有效缓存减少上游认证请求，到期后必须刷新；通过设置到期状态避免 sleep 导致测试缓慢或不稳定。
     *
     * <p>测试输入：连续发起 one、two 两次手机号请求，然后将缓存到期时间设为 0 再发起 three；两次 token 响应依次为 first、second。</p>
     * <p>预期结果：三次均返回 13800138000；前两次仅获取一次 token，第三次刷新后累计两次；请求 token 顺序为 first、first、second。</p>
     */
    @Test
    void accessTokenIsCachedAcrossPhoneRequestsAndRefreshedAfterExpiry() {
        when(restTemplate.getForObject(TOKEN_URL, String.class))
                .thenReturn("{\"access_token\":\"first\",\"expires_in\":7200}",
                        "{\"access_token\":\"second\",\"expires_in\":7200}");
        phoneResponse("{\"phone_info\":{\"purePhoneNumber\":\"13800138000\"}}");
        assertThat(util.getPhoneNumberByCode("one")).isEqualTo("13800138000");
        assertThat(util.getPhoneNumberByCode("two")).isEqualTo("13800138000");
        verify(restTemplate, times(1)).getForObject(TOKEN_URL, String.class);

        // Force expiration without sleeps or dependence on wall-clock scheduling.
        ReflectionTestUtils.setField(util, "accessTokenExpireAt", 0L);
        assertThat(util.getPhoneNumberByCode("three")).isEqualTo("13800138000");
        verify(restTemplate, times(2)).getForObject(TOKEN_URL, String.class);
        var captor = ArgumentCaptor.forClass(ClientRequest.class);
        verify(exchange, times(3)).exchange(captor.capture());
        assertThat(captor.getAllValues()).extracting(request -> request.url().getRawQuery())
                .containsExactly("access_token=first", "access_token=first", "access_token=second");
    }

    /**
     * 记录当前 max(expires_in-300, 60) 策略：预留 300 秒并设置最小 60 秒；此测试不证明异常短有效期下 token 真实可用。
     *
     * <p>测试输入：expires_in 与预期缓存秒数五组配对：7200→6900、301→60、300→60、0→60、-1→60。</p>
     * <p>预期结果：每组缓存到期时间落在调用起止时间分别加预期秒数的区间内。</p>
     *
     * @param expiresIn 微信返回的有效期（秒），分别为 7200、301、300、0、-1
     * @param lifetimeSeconds 扣除安全余量并应用最小值后的预期缓存时长（秒），依次为 6900、60、60、60、60
     */
    @ParameterizedTest
    @CsvSource({"7200,6900", "301,60", "300,60", "0,60", "-1,60"})
    void accessTokenExpiryUsesSafetyMarginAndMinimumLifetime(int expiresIn, long lifetimeSeconds) {
        tokenResponse("{\"access_token\":\"token\",\"expires_in\":" + expiresIn + "}");
        assertTokenLifetime(lifetimeSeconds);
    }

    /**
     * 缺失有效期时默认采用 7200 秒，再扣除 300 秒安全余量。
     *
     * <p>测试输入：token 响应包含 access_token=token，但不提供 expires_in。</p>
     * <p>预期结果：缓存到期时间为本次调用时间区间加 6900 秒。</p>
     */
    @Test
    void accessTokenDefaultsToTwoHourExpiry() {
        tokenResponse("{\"access_token\":\"token\"}");
        assertTokenLifetime(6900);
    }

    /**
     * 获取认证 token 失败后立即停止后续调用，并保留统一错误映射。
     *
     * <p>测试输入：token 响应 errcode 分别为 40029、40163、40013，errmsg=rejected。</p>
     * <p>预期结果：40029/40163 映射 WECHAT_INVALID_CODE，40013 映射 WECHAT_API_ERROR；消息为“微信 accessToken 失败: rejected”，手机号接口无调用。</p>
     *
     * @param error 微信 token 接口错误码：40029、40163 或 40013
     * @param expected 对应的预期业务错误分类：凭证无效或接口调用失败
     */
    @ParameterizedTest
    @CsvSource({"40029,WECHAT_INVALID_CODE", "40163,WECHAT_INVALID_CODE", "40013,WECHAT_API_ERROR"})
    void accessTokenMapsWechatErrorsAndSkipsPhoneRequest(int error, ErrorCode expected) {
        tokenResponse("{\"errcode\":" + error + ",\"errmsg\":\"rejected\"}");
        assertThatThrownBy(() -> util.getPhoneNumberByCode("code"))
                .isInstanceOfSatisfying(CidiException.class, ex -> assertThat(ex.getErrorCode()).isEqualTo(expected))
                .hasMessage("微信 accessToken 失败: rejected");
        verifyNoInteractions(exchange);
    }

    /**
     * 无效 token 不应进入缓存，后续请求应允许重新获取并恢复。
     *
     * <p>测试输入：首个响应分别为缺少 access_token、token 为空串、token 为空格，共 3 组；下次返回 recovered 并模拟手机号成功。</p>
     * <p>预期结果：首调抛出 WECHAT_API_ERROR 和“微信 access_token 缺失”，不调用手机号接口；再次调用成功返回 13800138000，token 获取累计两次。</p>
     *
     * @param json 首次获取 token 的无效响应，access_token 分别缺失、为空字符串或为空格
     */
    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"access_token\":\"\"}", "{\"access_token\":\" \"}"})
    void accessTokenRequiresNonblankTokenAndDoesNotCacheFailure(String json) {
        when(restTemplate.getForObject(TOKEN_URL, String.class)).thenReturn(json, "{\"access_token\":\"recovered\"}");
        assertThatThrownBy(() -> util.getPhoneNumberByCode("code"))
                .isInstanceOfSatisfying(CidiException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.WECHAT_API_ERROR))
                .hasMessage("微信 access_token 缺失");
        verifyNoInteractions(exchange);
        phoneResponse("{\"phone_info\":{\"purePhoneNumber\":\"13800138000\"}}");
        assertThat(util.getPhoneNumberByCode("new-code")).isEqualTo("13800138000");
        verify(restTemplate, times(2)).getForObject(TOKEN_URL, String.class);
    }

    /**
     * 已知缺陷记录：JSON null 被转成字符串并缓存发送；正确预期应拒绝缺失 token，且不调用手机号接口。
     *
     * <p>测试输入：token 响应含 access_token:null、expires_in=7200，连续两次手机号请求均模拟成功。</p>
     * <p>预期结果：当前实现两次均返回 13800138000；token 只获取一次，两次查询串均为 access_token=null。</p>
     */
    @Test
    void knownDefect_accessTokenCachesAndSendsJsonNullAsLiteralNullInsteadOfRejectingIt() {
        // Characterizes a defect: explicit JSON null should be rejected as a missing
        // token before the phone request, but asText() yields the nonblank string "null".
        tokenResponse("{\"access_token\":null,\"expires_in\":7200}");
        phoneResponse("{\"phone_info\":{\"purePhoneNumber\":\"13800138000\"}}");
        assertThat(util.getPhoneNumberByCode("first-code")).isEqualTo("13800138000");
        assertThat(util.getPhoneNumberByCode("second-code")).isEqualTo("13800138000");
        verify(restTemplate, times(1)).getForObject(TOKEN_URL, String.class);
        var captor = ArgumentCaptor.forClass(ClientRequest.class);
        verify(exchange, times(2)).exchange(captor.capture());
        assertThat(captor.getAllValues()).extracting(request -> request.url().getRawQuery())
                .containsExactly("access_token=null", "access_token=null");
    }

    /**
     * 上游认证响应不可用时阻断依赖该 token 的后续业务请求。
     *
     * <p>测试输入：token 响应分别为 Java null、空串、not-json、JSON 根 null，共 4 组。</p>
     * <p>预期结果：每组均抛出 WECHAT_API_ERROR，手机号接口无调用。</p>
     *
     * @param json 不可用的 token 响应，分别为 Java null、空字符串、not-json 和 JSON 根 null
     */
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"not-json", "null"})
    void accessTokenWrapsUnusableResponse(String json) {
        tokenResponse(json);
        assertError(() -> util.getPhoneNumberByCode("code"), ErrorCode.WECHAT_API_ERROR);
        verifyNoInteractions(exchange);
    }

    /**
     * 认证阶段传输失败应统一封装并中止后续手机号获取流程。
     *
     * <p>测试输入：获取 token 时模拟 RestTemplate 抛出 ResourceAccessException("offline")。</p>
     * <p>预期结果：抛出 WECHAT_API_ERROR，消息为“获取微信 access_token 失败”，保留原 cause，手机号接口无调用。</p>
     */
    @Test
    void accessTokenWrapsTransportFailureWithCause() {
        var cause = new ResourceAccessException("offline");
        when(restTemplate.getForObject(TOKEN_URL, String.class)).thenThrow(cause);
        assertThatThrownBy(() -> util.getPhoneNumberByCode("code"))
                .isInstanceOfSatisfying(CidiException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.WECHAT_API_ERROR))
                .hasMessage("获取微信 access_token 失败").hasCause(cause);
        verifyNoInteractions(exchange);
    }

    private void assertTokenLifetime(long seconds) {
        phoneResponse("{\"phone_info\":{\"purePhoneNumber\":\"13800138000\"}}");
        long before = System.currentTimeMillis();
        util.getPhoneNumberByCode("code");
        long after = System.currentTimeMillis();
        assertThat((Long) ReflectionTestUtils.getField(util, "accessTokenExpireAt"))
                .isBetween(before + seconds * 1000, after + seconds * 1000);
    }

    private void assertMockSession(String code, String normalized) {
        assertThat(util.getSessionInfoFromCode(code)).isEqualTo(new WeChatApiUtil.WeChatSessionInfo(
                "mock_" + normalized, null, "mock_session_" + normalized));
    }

    private void loginResponse(String json) {
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(json);
    }

    private void validToken() {
        tokenResponse("{\"access_token\":\"token\",\"expires_in\":7200}");
    }

    private void tokenResponse(String json) {
        when(restTemplate.getForObject(TOKEN_URL, String.class)).thenReturn(json);
    }

    private void phoneResponse(String json) {
        when(exchange.exchange(any())).thenReturn(Mono.just(ClientResponse.create(HttpStatus.OK)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE).body(json).build()));
    }

    private void assertError(Runnable action, ErrorCode expected) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(CidiException.class,
                ex -> assertThat(ex.getErrorCode()).isEqualTo(expected));
    }
}
