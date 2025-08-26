package cn.bitlinks.ems.module.power.controller.admin.statistics.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;


@Data
@NoArgsConstructor
@Schema(description = "管理后台 - 利用率 VO")
@JsonInclude(JsonInclude.Include.ALWAYS)
public class EnergyUtilizationRateInfo {

    @Schema(description = "数据", example = "数据")
    private List<EnergyUtilizationRateInfoData> energyUtilizationRateInfoDataList;

    @Schema(description = "周期合计", example = "0.00")
    private BigDecimal periodRate;

    @Schema(description = "数据项", example = "热力")
    private String itemName;

}