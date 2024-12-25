package cn.bitlinks.ems.module.power.controller.admin.statistics.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * @author liumingqiang
 */
@Schema(description = "统计总览 折标煤用量统计/折价统计底层VO")
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
public class StatisticsOverviewData {

    @Schema(description = "今日/本周/本季/本年")
    private StatisticsOverviewStatisticsData now;

    @Schema(description = "昨日/上周/上季/去年")
    private StatisticsOverviewStatisticsData previous;

    @Schema(description = "同比")
    private StatisticsOverviewStatisticsData YOY;

    @Schema(description = "环比")
    private StatisticsOverviewStatisticsData MOM;

    @Schema(description = "柱状图数据")
    private StatisticsBarVO bar;
}