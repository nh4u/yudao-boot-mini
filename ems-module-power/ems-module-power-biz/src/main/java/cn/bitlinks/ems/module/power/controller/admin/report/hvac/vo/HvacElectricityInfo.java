package cn.bitlinks.ems.module.power.controller.admin.report.hvac.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@Schema(description = "管理后台 - 暖通电量信息 VO")
@JsonInclude(JsonInclude.Include.ALWAYS)
public class HvacElectricityInfo {
    @Schema(description = "数据", example = "数据")
    private List<HvacElectricityInfoData> hvacElectricityInfoDataList;

    @Schema(description = "标签名", example = "热力")
    private String itemName;
    @Schema(description = "标签编码", example = "热力")
    private String itemCode;

    @Schema(description = "周期合计-当期", example = "4.44")
    private BigDecimal periodNow;

    @Schema(description = "周期合计-同期", example = "3.93")
    private BigDecimal periodPrevious;

    @Schema(description = "周期合计-同比", example = "12.98")
    private BigDecimal periodRatio;
}
