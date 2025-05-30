package cn.bitlinks.ems.module.power.controller.admin.statistics.vo;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author wangl
 * @date 2025年05月12日 11:11
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StatisticInfoDataV2 {


        @Schema(description = "日期", example = "2024-12-11 | 2024-12 | 2024")
        private String date;

        @Schema(description = "用量", example = "0.00")
        private BigDecimal consumption;

        @Schema(description = "金额（成本）", example = "0.00")
        private BigDecimal money;


}
