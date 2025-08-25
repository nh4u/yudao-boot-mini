package cn.bitlinks.ems.module.power.controller.admin.report.supplyanalysis.vo;

import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.PieChartVO;
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
public class SupplyAnalysisPieResultVO {

    /**
     * PCW供水量
     */
    @Schema(description = "PCW供水量")
    private PieChartVO pcwPie;
    /**
     * 低温水供水量
     */
    @Schema(description = "低温水供水量")
    private PieChartVO ltwPie;
    /**
     *
     * 中温水供水量
     */
    @Schema(description = "中温水供水量")
    private PieChartVO mtwPie;
    /**
     * 热回收水/温水供水量
     */
    @Schema(description = "热回收水/温水供水量")
    private PieChartVO hrwPie;
    /**
     * 锅炉热水供水量
     */
    @Schema(description = "锅炉热水供水量")
    private PieChartVO bhwPie;
    /**
     * 市政热水供水量
     */
    @Schema(description = "市政热水供水量")
    private PieChartVO mhwPie;
    /**
     * 数据最后更新时间
     */
    @Schema(description = "数据最后更新时间", example = "2025-05-25 14:39:53")
    private LocalDateTime dataTime;
}
