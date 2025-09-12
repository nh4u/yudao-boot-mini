package cn.bitlinks.ems.module.power.controller.admin.bigscreen.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * @author liumingqiang
 */
@Schema(description = "管理后台 - 纯废水压缩空气设置新增/修改 Request VO")
@Data
public class PowerPureWasteWaterGasSettingsSaveReqVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "3687")
    @NotNull(message = "id不能为空")
    private Long id;

    @Schema(description = "类型", example = "随便")
    @NotBlank(message = "类型不能为空")
    private String system;

    @Schema(description = "编号", example = "随便")
    @NotBlank(message = "编号不能为空")
    private String code;

    @Schema(description = "能源codes", example = "随便")
    @NotBlank(message = "能源codes不能为空")
    private String energyCodes;

    @Schema(description = "名称", example = "王五")
    @NotBlank(message = "名称不能为空")
    private String name;

    @Schema(description = "计量器具ids")
    @NotBlank(message = "计量器具不能为空")
    private String standingbookIds;
}