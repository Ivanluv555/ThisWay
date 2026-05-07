/**
 * NextDestinationDTO
 *
 * 说明：下一目的地预测响应DTO
 */

package org.daiso.cidi.modules.locationtransition.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NextDestinationDTO {

    private String hour;  // "14:00"
    private List<TransitionItem> transitions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransitionItem {
        private Long locationId;
        private String locationName;
        private Double ratio;           // 0.42
        private String ratioText;       // "42%"
        private Integer users;          // 25
    }
}
