package cn.bitlinks.ems.module.power.controller.admin.statistics.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "定基比统计项")
@JsonInclude(JsonInclude.Include.ALWAYS)
public class BaseItemVO extends StatisticsInfoBase {

    @Schema(description = "每日同比详情列表")
    private List<BaseDetailVO> statisticsRatioDataList;

    @Schema(description = "当前周期总值", example = "533.14")
    private BigDecimal sumNow;

    @Schema(description = "上周期总值", example = "472.54")
    private BigDecimal sumPrevious;

    @Schema(description = "总同比百分比", example = "12.82")
    private BigDecimal sumRatio;
}
