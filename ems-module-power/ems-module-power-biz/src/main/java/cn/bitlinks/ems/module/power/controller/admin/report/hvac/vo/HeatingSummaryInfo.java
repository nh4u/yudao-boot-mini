package cn.bitlinks.ems.module.power.controller.admin.report.hvac.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@Schema(description = "管理后台 - 热力汇总结果信息 VO")
@JsonInclude(JsonInclude.Include.ALWAYS)
public class HeatingSummaryInfo {

    @Schema(description = "数据", example = "数据")
    private List<HeatingSummaryInfoData> heatingSummaryInfoDataList;

    @Schema(description = "周期合计", example = "0.00")
    private BigDecimal periodSum;

    @Schema(description = "数据项", example = "热力")
    private String itemName;

}