package org.daiso.cidi.modules.locationhourlyflow.dto;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;

class LocationHourlyFlowDTOTest {
    /**
     * 客流传输对象缺省来源应为模拟数据，显式传入来源应原样保留。
     * 输入无参对象、缺省构建器和显式来源，预期前两者使用默认值，显式空值及真实来源不被覆盖。
     */
    @Test
    void defaultSourceDoesNotOverrideExplicitSource() {
        assertThat(new LocationHourlyFlowDTO().getDataSource()).isEqualTo("simulated");
        assertThat(LocationHourlyFlowDTO.builder().build().getDataSource()).isEqualTo("simulated");
        assertThat(LocationHourlyFlowDTO.builder().dataSource("sensor").build().getDataSource()).isEqualTo("sensor");
        assertThat(LocationHourlyFlowDTO.builder().dataSource(null).build().getDataSource()).isNull();
    }

    /**
     * 客流小时应按接口约定输出到秒并支持反序列化。
     * 输入固定小时和缺省来源，预期序列化时间采用无时区字符串，反序列化保留时间和默认来源。
     * @throws Exception JSON序列化或反序列化失败时抛出
     */
    @Test
    void recordHourUsesContractFormatAndRoundTrips() throws Exception {
        JsonMapper mapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();
        LocalDateTime hour = LocalDateTime.of(2026, 1, 2, 8, 9, 10);
        String json = mapper.writeValueAsString(LocationHourlyFlowDTO.builder().recordHour(hour).build());
        assertThat(mapper.readTree(json).get("recordHour").asText()).isEqualTo("2026-01-02T08:09:10");
        assertThat(mapper.readValue(json, LocationHourlyFlowDTO.class).getRecordHour()).isEqualTo(hour);
        assertThat(mapper.readValue("{}", LocationHourlyFlowDTO.class).getDataSource()).isEqualTo("simulated");
        assertThat(mapper.readValue("{\"dataSource\":null}", LocationHourlyFlowDTO.class).getDataSource()).isNull();
    }
}
