package org.daiso.cidi.modules.location.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 景点基础信息的数据传输模型，包含对应记录的标识及业务字段。
 *
 * <p>保存景点名称、地址、介绍、热度、分类文本、评分和经纬度，供景点展示及客流模块引用。
 *
 * <p>当前 {@code LocationController} 的 CRUD 接口直接收发实体，此 DTO 尚未接入该调用链。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationDTO {


    private Long id;
    private String title;
    private String address;
    private String description;
    private Integer heatScore;
    private String category;
    private BigDecimal rating;
    private BigDecimal latitude;
    private BigDecimal longitude;

}
