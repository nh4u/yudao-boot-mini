package cn.bitlinks.ems.module.power.controller.admin.statistics.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;

/**
 * @author liumingqiang
 */
@Schema(description = "管理后台 - 用能结构分析返回结果 VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
public class StatisticsStructureData {


    @Schema(description = "日期", example = "2024-12-11 | 2024-12 | 2024")
    private String date;

    @Schema(description = "折标煤/折价", example = "0.00")
    private BigDecimal num;

    @Schema(description = "占比", example = "0.00")
        private BigDecimal proportion;

}