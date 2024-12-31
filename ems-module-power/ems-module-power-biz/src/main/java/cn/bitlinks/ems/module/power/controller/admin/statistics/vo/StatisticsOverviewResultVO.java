package cn.bitlinks.ems.module.power.controller.admin.statistics.vo;

import com.alibaba.fastjson.JSONObject;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @author liumingqiang
 */
@Schema(description = "管理后台 - 用能统计返回结果 VO")
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
public class StatisticsOverviewResultVO {


    @Schema(description = "计量器具总数", example = "计量器具总数")
    private Long measurementInstrumentNum;

    @Schema(description = "重点设备总数", example = "重点设备总数")
    private Long keyEquipmentNum;

    @Schema(description = "其他设备总数", example = "其他设备总数")
    private Long otherEquipmentNum;

    @Schema(description = "单位产值能耗", example = "单位产值能耗")
    private BigDecimal outputValueEnergyConsumption;

    @Schema(description = "单位产品能耗（8英寸）", example = "单位产品能耗（8英寸）")
    private BigDecimal productEnergyConsumption8;

    @Schema(description = "单位产品能耗（12英寸）", example = "单位产品能耗（12英寸）")
    private BigDecimal productEnergyConsumption12;

    @Schema(description = "能源利用率（外购）", example = "能源利用率（外购）")
    private BigDecimal outsourceEnergyUtilizationRate;

    @Schema(description = "能源利用率（园区）", example = "能源利用率（园区）")
    private BigDecimal parkEnergyUtilizationRate;

    @Schema(description = "能源转换率", example = "能源转换率")
    private BigDecimal energyConversionRate;

    @Schema(description = "能源列表数据", example = "能源列表数据")
    private List<StatisticsOverviewEnergyData> statisticsOverviewEnergyDataList;

    @Schema(description = "折标煤用量统计", example = "折标煤用量统计",hidden = true)
    private StatisticsOverviewData standardCoalStatistics;

    @Schema(description = "折价统计", example = "折价统计",hidden = true)
    private StatisticsOverviewData moneyStatistics;

    @Schema(description = "折标煤用量统计（new）", example = "折标煤用量统计")
    private JSONObject standardCoalStatisticsJson;

    @Schema(description = "折价统计（new）", example = "折价统计")
    private JSONObject moneyStatisticsJson;

    @Schema(description = "数据更新时间", example = "数据更新时间")
    private LocalDateTime dataUpdateTime;
}