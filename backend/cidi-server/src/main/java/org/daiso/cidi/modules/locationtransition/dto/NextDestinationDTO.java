package org.daiso.cidi.modules.locationtransition.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 下一目的地展示响应，包含一个小时标签和该小时的候选转移目标列表。
 *
 * <p>由 LocationTransitionService 使用起点最新预测时刻的前三个目标组装；多个小时条目复用同一组概率和人数，仅小时标签递增。
 *
 * <p>hour 使用 HH:mm，不包含日期；比例同时保留数值 ratio 和取整后的百分比文本 ratioText。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NextDestinationDTO {

    /** 小时标签，例如 14:00；跨天时仍只输出时分。 */
    private String hour;
    private List<TransitionItem> transitions;

    /**
     * 单个候选目的地的展示信息，包含目标景点、转移比例及预计人数。
     *
     * <p>ratio 为原始概率的小数表示，ratioText 为乘以 100 后四舍五入的百分比文本；
     * users 直接取外部预测的 expectedUsers，服务不会按小时重新计算。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransitionItem {
        private Long locationId;
        private String locationName;
        private Double ratio;
        private String ratioText;
        private Integer users;
    }
}
