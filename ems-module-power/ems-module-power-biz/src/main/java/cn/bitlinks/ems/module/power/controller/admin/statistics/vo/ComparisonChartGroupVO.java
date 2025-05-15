package cn.bitlinks.ems.module.power.controller.admin.statistics.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "图表统计 - 单组图表数据")
@Data
public class ComparisonChartGroupVO {

    @Schema(description = "分组名称")
    private String name;

    @Schema(description = "横轴时间数据")
    private List<String> xdata;

    @Schema(description = "Y轴序列数据")
    private List<ChartSeriesItemVO> ydata;

    @Schema(description = "数据时间")
    private LocalDateTime dataTime;
}
