package cn.bitlinks.ems.module.power.controller.admin.report.electricity.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;


/**
 * @author liumingqiang
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductionConsumptionStatisticInfoData {
    @Schema(description = "日期", example = "2024-12-11 | 2024-12 | 2024")
    private String date;

    @Schema(description = "用量", example = "0.00")
    private BigDecimal consumption;
}
