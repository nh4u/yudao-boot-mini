package cn.bitlinks.ems.module.power.controller.admin.report.hvac.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@Schema(description = "管理后台 - 变压器利用率 VO")
public class TransformerUtilizationInfo {

    @Schema(description = "数据", example = "数据")
    private List<TransformerUtilizationInfoData> transformerUtilizationInfoData;

    @Schema(description = "变压器")
    private String transformerName;
    @Schema(description = "变压器id")
    private Long transformerId;

    @Schema(description = "分类")
    private String type;

    @Schema(description = "下级分类")
    private String childType;

    @Schema(description = "周期合计-实际负载")
    private BigDecimal periodActualLoad;

    @Schema(description = "周期合计-利用率")
    private BigDecimal periodUtilization;

}
