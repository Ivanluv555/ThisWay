package org.daiso.cidi.core.error;

import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorResponseTest {

    private final ObjectMapper objectMapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();

    /**
     * 确认统一错误响应对象完整承载错误发生时间、分类、说明和请求位置。
     *
     * <p>测试输入：时间 2026-11-08T14:30:45、状态 400、错误码 REQ-400、消息“请求参数错误”、路径 /api/items。</p>
     * <p>预期结果：ErrorResponse 的五个访问器分别返回传入值。</p>
     */
    @Test
    void recordExposesAllFields() {
        LocalDateTime timestamp = LocalDateTime.of(2026, 11, 8, 14, 30, 45);

        ErrorResponse response = new ErrorResponse(timestamp, 400, "REQ-400", "请求参数错误", "/api/items");

        assertThat(response.timestamp()).isEqualTo(timestamp);
        assertThat(response.status()).isEqualTo(400);
        assertThat(response.code()).isEqualTo("REQ-400");
        assertThat(response.message()).isEqualTo("请求参数错误");
        assertThat(response.path()).isEqualTo("/api/items");
    }

    /**
     * 确认在本测试指定的序列化配置下，错误响应字段名称、类型及往返转换保持稳定。
     *
     * <p>测试输入：包含固定时间、422、REQ-422、field: invalid、/api/items 的响应；使用已注册 JavaTimeModule 且关闭时间戳数组的 ObjectMapper。</p>
     * <p>预期结果：JSON 恰有五个约定字段，timestamp 为 ISO 时间字符串，status 为整数，其余字段值不变；反序列化后等于原对象。</p>
     *
     * @throws Exception 测试中的 JSON 序列化或反序列化发生非预期错误时
     */
    @Test
    void serializesAllFieldsWithExpectedNamesAndValues() throws Exception {
        LocalDateTime timestamp = LocalDateTime.of(2026, 11, 8, 14, 30, 45);
        ErrorResponse response = new ErrorResponse(timestamp, 422, "REQ-422", "field: invalid", "/api/items");

        String json = objectMapper.writeValueAsString(response);

        JsonNode tree = objectMapper.readTree(json);
        assertThat(tree.size()).isEqualTo(5);
        assertThat(tree.get("timestamp").asText()).isEqualTo("2026-11-08T14:30:45");
        assertThat(tree.get("status").isIntegralNumber()).isTrue();
        assertThat(tree.get("status").asInt()).isEqualTo(422);
        assertThat(tree.get("code").asText()).isEqualTo("REQ-422");
        assertThat(tree.get("message").asText()).isEqualTo("field: invalid");
        assertThat(tree.get("path").asText()).isEqualTo("/api/items");
        assertThat(objectMapper.readValue(json, ErrorResponse.class)).isEqualTo(response);
    }

    /**
     * 记录对象序列化对空字段的兼容性；不代表正常错误响应应该缺少这些信息。
     *
     * <p>测试输入：timestamp、message、path 为 null，status 为 500，code 为 SYS-500 的 ErrorResponse。</p>
     * <p>预期结果：序列化成功，JSON 中 timestamp、message、path 三个字段均保留 null。</p>
     *
     * @throws Exception 测试中的 JSON 序列化或解析发生非预期错误时
     */
    @Test
    void supportsNullOptionalFieldValuesDuringSerialization() throws Exception {
        ErrorResponse response = new ErrorResponse(null, 500, "SYS-500", null, null);

        String json = objectMapper.writeValueAsString(response);

        assertThat(objectMapper.readTree(json).get("timestamp").isNull()).isTrue();
        assertThat(objectMapper.readTree(json).get("message").isNull()).isTrue();
        assertThat(objectMapper.readTree(json).get("path").isNull()).isTrue();
    }
}
