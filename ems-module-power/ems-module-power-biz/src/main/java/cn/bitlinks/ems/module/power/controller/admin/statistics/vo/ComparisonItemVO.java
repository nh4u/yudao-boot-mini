package cn.bitlinks.ems.module.power.controller.admin.statistics.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "环比统计项")
public class ComparisonItemVO extends StatisticsInfoBase {

    @Schema(description = "每日环比详情列表")
    private List<ComparisonDetailVO> statisticsRatioDataList;

    @Schema(description = "当前周期总值", example = "533.14")
    private BigDecimal sumNow;

    @Schema(description = "上周期总值", example = "472.54")
    private BigDecimal sumPrevious;

    @Schema(description = "总环比百分比", example = "12.82")
    private BigDecimal sumRatio;
}
