package cn.bitlinks.ems.module.power.controller.admin.report.hvac.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransformerUtilizationInfoData {
    @Schema(description = "日期", example = "2024-12-11 | 2024-12 | 2024")
    private String date;

    @Schema(description = "实际负载", example = "3.93")
    private BigDecimal actualLoad;

    @Schema(description = "利用率", example = "12.98")
    private BigDecimal utilization;
}
