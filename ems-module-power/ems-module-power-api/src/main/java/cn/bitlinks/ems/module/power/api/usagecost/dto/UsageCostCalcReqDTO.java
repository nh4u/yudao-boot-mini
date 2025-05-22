package cn.bitlinks.ems.module.power.api.usagecost.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;

/**
 * @author wangl
 * @date 2025年05月19日 16:40
 */
@Data
public class UsageCostCalcReqDTO {

    /**
     * 台账ID
     */
    private Long standingbookId;

    /**
     * 数据聚合时间
     */
    private LocalDateTime aggregateTime;

    /**
     * 当前用量
     */
    private BigDecimal currentUsage;
}
