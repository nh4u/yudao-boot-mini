package cn.bitlinks.ems.module.power.controller.admin.report.hvac.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.ALWAYS)
public class HvacElectricityInfoData {
    @Schema(description = "日期", example = "2024-12-11 | 2024-12 | 2024")
    private String date;

    @Schema(description = "当期值", example = "4.44")
    private BigDecimal now;

    @Schema(description = "同期值", example = "3.93")
    private BigDecimal previous;

    @Schema(description = "同比百分比", example = "12.98")
    private BigDecimal ratio;
}
