package cn.bitlinks.ems.module.power.controller.admin.statistics.vo;

/**
 * @author wangl
 * @date 2025年05月15日 10:57
 */

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author wangl
 * @date 2025年05月14日 15:06
 */
@Schema(description = "管理后台 - 用能统计结果图 VO")
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
public class StatisticsChartPieResultVO {

    /**
     * 按能源查看pie
     */
    @Schema(description = "按能源查看pie")
    private List<PieChartVO> energyPies;
    /**
     * 按标签查看pie
     */
    @Schema(description = "按标签查看pie")
    private List<PieChartVO> labelPies;
    /**
     * 按综合查看pie-能源
     */
    @Schema(description = "按综合查看pie-能源")
    private PieChartVO energyPie;
    /**
     * 按综合查看pie-标签
     */
    @Schema(description = "按综合查看pie-标签")
    private PieChartVO labelPie;
    /**
     * 数据最后更新时间
     */
    @Schema(description = "数据最后更新时间", example = "2025-05-25 14:39:53")
    private LocalDateTime dataTime;
}
