package cn.bitlinks.ems.module.power.controller.admin.report.electricity.vo;

import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsInfoBase;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author bmqi
 * @date 2025年07月30日 18:19
 */
@Data
@NoArgsConstructor
@Schema(description = "管理后台 - 用电量统计结果信息 VO")
@JsonInclude(JsonInclude.Include.ALWAYS)
public class ConsumptionStatisticsInfo extends StatisticsInfoBase {

        @Schema(description = "数据", example = "数据")
        private List<ConsumptionStatisticsInfoData> statisticsDateDataList;

        @Schema(description = "合计用量", example = "0.00")
        private BigDecimal sumEnergyConsumption;
}
