package org.daiso.cidi.modules.locationhourlyflow.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;

/**
 * 小时客流接口的请求与响应模型，用于批量写入、历史查询及最新状态查询。
 *
 * <p>携带景点 ID、记录时间、人数、拥挤指数及来源；recordHour 按不含时区偏移的秒级格式序列化。
 *
 * <p>dataSource 默认是 simulated；保存时由服务补齐空来源和空拥挤指数，数据库创建时间不包含在此模型中。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationHourlyFlowDTO {

    private Long id;

    private Long locationId;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime recordHour;

    private Integer visitorCount;

    private BigDecimal congestionIndex;

    @Builder.Default
    private String dataSource = "simulated";
}
