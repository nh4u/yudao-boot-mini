package cn.bitlinks.ems.module.power.controller.admin.report.electricity.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;


/**
 * @author liumingqiang
 */
@Data
@NoArgsConstructor
@Schema(description = "管理后台 - 供水温度统计结果信息 VO")
public class ProductionConsumptionStatisticsInfo {

    @Schema(description = "id 用于排序")
    private Long id;

    @Schema(description = "统计项")
    private String name;

    @Schema(description = "数据", example = "数据")
    private List<ProductionConsumptionStatisticInfoData> statisticsDateDataList;

    @Schema(description = "合计用量", example = "0.00")
    private BigDecimal sumConsumption;

}
