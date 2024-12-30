package cn.bitlinks.ems.module.power.controller.admin.statistics.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * @author liumingqiang
 */
@Schema(description = "统计总览 能源消耗 VO")
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
public class StatisticsOverviewEnergyData {

    @Schema(description = "名称", example = "天然气")
    private String name;

    @Schema(description = "用量", example = "0.00")
    private BigDecimal consumption;

    @Schema(description = "折标煤", example = "0.00")
    private BigDecimal standardCoal;

    @Schema(description = "折价", example = "0.00")
    private BigDecimal money;

}