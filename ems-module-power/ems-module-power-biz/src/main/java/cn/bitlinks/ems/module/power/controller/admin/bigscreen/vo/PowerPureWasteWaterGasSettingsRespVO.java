package cn.bitlinks.ems.module.power.controller.admin.bigscreen.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author liumingqiang
 */
@Schema(description = "管理后台 - 纯废水压缩空气设置 Response VO")
@Data
@ExcelIgnoreUnannotated
@JsonInclude(JsonInclude.Include.ALWAYS)
public class PowerPureWasteWaterGasSettingsRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "3687")
    private Long id;

    @Schema(description = "类型", example = "随便")
    private String system;

    @Schema(description = "编号", example = "随便")
    private String code;

    @Schema(description = "能源codes", example = "随便")
    private String energyCodes;

    @Schema(description = "名称", example = "王五")
    private String name;

    @Schema(description = "台账ids")
    private String standingbookIds;
}