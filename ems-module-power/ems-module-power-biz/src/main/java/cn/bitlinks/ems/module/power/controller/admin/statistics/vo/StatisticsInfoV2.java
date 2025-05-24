package cn.bitlinks.ems.module.power.controller.admin.statistics.vo;

import java.math.BigDecimal;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author wangl
 * @date 2025年05月12日 11:10
 */
@Data
@NoArgsConstructor
@Schema(description = "管理后台 - 用能统计结果信息 VO")
public class StatisticsInfoV2 extends StatisticsInfoBase{

        @Schema(description = "数据", example = "数据")
        private List<StatisticInfoDataV2> statisticsDateDataList;

        @Schema(description = "合计用量", example = "0.00")
        private BigDecimal sumEnergyConsumption;

        @Schema(description = "合计成本", example = "0.00")
        private BigDecimal sumEnergyMoney;
}
