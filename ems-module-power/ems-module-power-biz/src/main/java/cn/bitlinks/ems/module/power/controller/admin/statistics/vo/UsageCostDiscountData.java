package cn.bitlinks.ems.module.power.controller.admin.statistics.vo;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;


@Data
@NoArgsConstructor
public class UsageCostDiscountData {
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
     * 折价
     */
    private BigDecimal totalDiscount;


}
