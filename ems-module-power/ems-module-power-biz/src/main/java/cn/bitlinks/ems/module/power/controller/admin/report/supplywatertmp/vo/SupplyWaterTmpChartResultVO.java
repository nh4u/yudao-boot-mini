package cn.bitlinks.ems.module.power.controller.admin.report.supplywatertmp.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * @author wangl
 * @date 2025年05月14日 15:06
 */
@Schema(description = "管理后台 - 用能统计结果图 VO")
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
public class SupplyWaterTmpChartResultVO {

    @Schema(description = "低温水供水温度")
    private LtwtChartVO ltwt;

    @Schema(description = "中温水供水温度")
    private LtwtChartVO mtwt;

    @Schema(description = "温水供水温度")
    private LtwtChartVO hrwt;

    @Schema(description = "锅炉热水供水温度")
    private PcwChartVO bhwt;

    @Schema(description = "PCW压力温度供应分析")
    private PcwChartVO pcwp;

    @Schema(description = "数据最后更新时间", example = "2025-05-25 14:39:53")
    private LocalDateTime dataTime;
}
