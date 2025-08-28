package cn.bitlinks.ems.module.power.controller.admin.statistics.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "图表统计 - 环比图表返回结果")
@Data
@JsonInclude(JsonInclude.Include.ALWAYS)
public class ComparisonChartResultVO {

    @Schema(description = "图表列表")
    private List<ComparisonChartGroupVO> list;

    @Schema(description = "数据时间")
    private LocalDateTime dataTime;
}
