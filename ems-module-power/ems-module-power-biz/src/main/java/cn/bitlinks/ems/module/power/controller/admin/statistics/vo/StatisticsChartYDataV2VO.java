package cn.bitlinks.ems.module.power.controller.admin.statistics.vo;


import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author wangl
 * @date 2025年05月14日 15:18
 */
@Schema(description = "堆叠图Y轴数据")
@Data
public class StatisticsChartYDataV2VO {

    @Schema(description = "最大成本")
    private BigDecimal max;

    @Schema(description = "最小成本")
    private BigDecimal min;

    @Schema(description = "成本总和")
    private BigDecimal cost;

    @Schema(description = "成本平均值")
    private BigDecimal avg;
}
