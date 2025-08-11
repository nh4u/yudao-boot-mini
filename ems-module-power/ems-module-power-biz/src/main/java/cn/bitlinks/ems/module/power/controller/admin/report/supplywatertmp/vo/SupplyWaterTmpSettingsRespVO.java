package cn.bitlinks.ems.module.power.controller.admin.report.supplywatertmp.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;

/**
 * @author liumingqiang
 */
@Schema(description = "管理后台 - 供应分析 Response VO")
@Data
@ExcelIgnoreUnannotated
public class SupplyWaterTmpSettingsRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "12042")
    private Long id;

    @Schema(description = "标识")
    private String code;

    @Schema(description = "系统")
    @NotEmpty(message = "系统不能为空")
    private String system;

    @Schema(description = "台账id")
    private Long standingbookId;

    @Schema(description = "能源参数名称")
    private String energyParamName;

    @Schema(description = "上限")
    private Integer max;

    @Schema(description = "下限")
    private Integer min;


}