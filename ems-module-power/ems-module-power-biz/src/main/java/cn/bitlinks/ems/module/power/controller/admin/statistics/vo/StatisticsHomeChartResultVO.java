package cn.bitlinks.ems.module.power.controller.admin.statistics.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * @author wangl
 * @date 2025年05月10日 16:35
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StatisticsHomeChartResultVO {

    /**
     * 时间
     */
    private String time;

    private BigDecimal accCost;

    private BigDecimal avgCost;

    private BigDecimal avgCoal;

    private BigDecimal accCoal;


}
