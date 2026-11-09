package org.daiso.cidi.modules.hotsearch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 热搜关键词的数据传输模型，包含对应记录的标识及业务字段。
 *
 * <p>保存关键词、热度值 hotValue 和启用标记 isActive，作为搜索入口的展示数据。
 *
 * <p>当前 {@code HotSearchController} 的 CRUD 接口直接收发实体，此 DTO 尚未接入该调用链。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HotSearchDTO {


    private Integer id;
    private String keyword;
    private Integer hotValue;
    private Integer isActive;

}
