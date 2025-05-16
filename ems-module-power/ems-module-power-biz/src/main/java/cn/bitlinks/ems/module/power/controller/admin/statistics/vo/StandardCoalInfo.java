package cn.bitlinks.ems.module.power.controller.admin.statistics.vo;

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
@Schema(description = "管理后台 - 用能统计结果信息 VO")
public class StandardCoalInfo extends StatisticsInfoBase {

    @Schema(description = "数据", example = "数据")
    private List<StandardCoalInfoData> standardCoalInfoDataList;

    @Schema(description = "合计用量", example = "0.00")
    private BigDecimal sumEnergyConsumption;

    @Schema(description = "合计折标煤", example = "0.00")
    private BigDecimal sumEnergyStandardCoal;
}
