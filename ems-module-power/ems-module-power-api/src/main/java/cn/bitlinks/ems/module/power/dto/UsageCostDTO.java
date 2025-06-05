package cn.bitlinks.ems.module.power.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;

/**
 * @author wangl
 * @date 2025年05月27日 16:12
 */
@Data
public class UsageCostDTO {
    /**
     * 台账ID
     */
    @JsonProperty("standingbook_id")
    private Long standingbookId;

    /**
     * 聚合时间
     */
    @JsonProperty("aggregate_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime aggregateTime;

    /**
     * 当前用量
     */
    @JsonProperty("current_usage")
    private BigDecimal currentUsage;

    /**
     * 截至当前总用量
     */
    @JsonProperty("total_usage")
    private BigDecimal totalUsage;

    /**
     * 成本
     */
    @JsonProperty("cost")
    private BigDecimal cost;

    /**
     * 折标煤
     */
    @JsonProperty("standard_coal_equivalent")
    private BigDecimal standardCoalEquivalent;

    /**
     * 能源
     */
    @JsonProperty("energy_id")
    private Long energyId;


}
