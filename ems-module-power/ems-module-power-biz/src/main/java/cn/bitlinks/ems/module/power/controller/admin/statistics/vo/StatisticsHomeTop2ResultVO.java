package cn.bitlinks.ems.module.power.controller.admin.statistics.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 首页顶部2层返回结果 VO")
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
@JsonInclude(JsonInclude.Include.ALWAYS)
public class StatisticsHomeTop2ResultVO {

    @Schema(description = "单位产值能耗", example = "单位产值能耗")
    private StatisticsHomeTop2Data outputValueEnergyConsumption;

    @Schema(description = "单位产品能耗（8英寸）", example = "单位产品能耗（8英寸）")
    private StatisticsHomeTop2Data productEnergyConsumption8;

    @Schema(description = "单位产品能耗（12英寸）", example = "单位产品能耗（12英寸）")
    private StatisticsHomeTop2Data productEnergyConsumption12;

    @Schema(description = "能源利用率（外购）", example = "能源利用率（外购）")
    private StatisticsHomeTop2Data outsourceEnergyUtilizationRate;

    @Schema(description = "能源利用率（园区）", example = "能源利用率（园区）")
    private StatisticsHomeTop2Data parkEnergyUtilizationRate;

    @Schema(description = "能源转换率", example = "能源转换率")
    private StatisticsHomeTop2Data energyConversionRate;

}
