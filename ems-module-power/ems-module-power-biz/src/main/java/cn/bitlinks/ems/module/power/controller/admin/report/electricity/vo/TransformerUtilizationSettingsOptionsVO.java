package cn.bitlinks.ems.module.power.controller.admin.report.electricity.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 变压器下拉
 */
@Data
public class TransformerUtilizationSettingsOptionsVO {

    @Schema(description = "变压器:名称（编码）")
    private String transformerLabel;

    @Schema(description = "变压器名称")
    private String transformerName;

    @Schema(description = "变压器id")
    private Long transformerId;
}
