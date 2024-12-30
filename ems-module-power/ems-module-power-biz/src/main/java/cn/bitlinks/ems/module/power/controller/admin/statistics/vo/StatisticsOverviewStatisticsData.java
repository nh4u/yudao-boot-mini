package cn.bitlinks.ems.module.power.controller.admin.statistics.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * @author liumingqiang
 */
@Schema(description = "统计总览 折标煤用量统计/折价统计底层VO")
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
public class StatisticsOverviewStatisticsData {

    @Schema(description = "累计", example = "0.00")
    private BigDecimal accumulate;

    @Schema(description = "最高(Max)", example = "0.00")
    private BigDecimal max;

    @Schema(description = "最低(Min)", example = "0.00")
    private BigDecimal min;

    @Schema(description = "平均(Avg)", example = "0.00")
    private BigDecimal average;

}