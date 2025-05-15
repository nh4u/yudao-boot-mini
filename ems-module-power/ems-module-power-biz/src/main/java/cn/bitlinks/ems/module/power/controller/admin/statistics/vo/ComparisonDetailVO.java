package cn.bitlinks.ems.module.power.controller.admin.statistics.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "环比每日详情")
public class ComparisonDetailVO {

    @Schema(description = "日期", example = "2025-05-12")
    private String date;

    @Schema(description = "当期值", example = "4.44")
    private BigDecimal now;

    @Schema(description = "上期值", example = "3.93")
    private BigDecimal previous;

    @Schema(description = "环比百分比", example = "12.98")
    private BigDecimal ratio;
}
