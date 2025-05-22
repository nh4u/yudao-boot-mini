package cn.bitlinks.ems.module.power.controller.admin.statistics.vo;

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
public class StandardCoalInfoData {


        @Schema(description = "日期", example = "2024-12-11 | 2024-12 | 2024")
        private String date;

        @Schema(description = "用量", example = "0.00")
        private BigDecimal consumption;

        @Schema(description = "折标煤", example = "0.00")
        private BigDecimal standardCoal;


}
