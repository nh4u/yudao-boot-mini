package cn.bitlinks.ems.module.power.controller.admin.statistics.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author liumingqiang
 */
@Schema(description = "管理后台 - 用能统计返回结果 VO")
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
public class StatisticsResultVO {


    @Schema(description = "一级标签", example = "1#厂房")
    private String label1;

    @Schema(description = "二级标签", example = "一层")
    private String label2;

    @Schema(description = "三级标签", example = "101室")
    private String label3;

    @Schema(description = "能源", example = "能源")
    private String energyName;

    @Schema(description = "任意级标签Id", example = "1", hidden = true)
    private Long labelId;

    @Schema(description = "能源Id", example = "1", hidden = true)
    private Long energyId;

    @Schema(description = "能源", example = "能源")
    private List<StatisticsDateData> statisticsDateDataList;

    @Schema(description = "合计能源用量", example = "0.00")
    private BigDecimal sumEnergyConsumption;

    @Schema(description = "合计能源折价", example = "0.00")
    private BigDecimal sumEnergyMoney;

    @Schema(description = "合计标签用量", example = "0.00")
    private BigDecimal sumLabelConsumption;

    @Schema(description = "合计标签折价", example = "0.00")
    private BigDecimal sumLabelMoney;
}