package cn.bitlinks.ems.module.power.controller.admin.statistics.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author wangl
 * @date 2025年05月10日 16:35
 */
@Data
@NoArgsConstructor
public class UsageCostData {
    /**
     * 台账ID，综合查看和按标签查看
     */
    private Long standingbookId;

    /**
     * 能源，按能源查看才有此字段
     */
    private Long energyId;


    /**
     * 时间
     */
    private String time;

    /**
     * 当前用量
     */
    private BigDecimal currentTotalUsage;


    /**
     * 成本
     */
    private BigDecimal totalCost;




}
