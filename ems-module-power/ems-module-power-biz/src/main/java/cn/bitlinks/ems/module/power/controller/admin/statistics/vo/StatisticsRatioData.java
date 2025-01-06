package cn.bitlinks.ems.module.power.controller.admin.statistics.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;

/**
 * @author liumingqiang
 */
@Schema(description = "管理后台 - 用能统计返回结果 VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
public class StatisticsRatioData {


    @Schema(description = "日期", example = "2024-12-11 | 2024-12 | 2024")
    private String date;

    @Schema(description = "当期", example = "0.00")
    private BigDecimal now;

    @Schema(description = "同期/上期", example = "0.00")
    private BigDecimal previous;

    @Schema(description = "同比/环比/定基比", example = "0.00%")
    private BigDecimal ratio;

}