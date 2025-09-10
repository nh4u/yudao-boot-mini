package cn.bitlinks.ems.module.power.controller.admin.bigscreen.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * @author liumingqiang
 */
@Schema(description = "管理后台 - 纯废水压缩空气设置 Response VO")
@Data
@ExcelIgnoreUnannotated
@JsonInclude(JsonInclude.Include.ALWAYS)
public class PowerMonthPlanSettingsRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "3687")
    @NotNull(message = "id不能为空")
    private Long id;

    @Schema(description = "能源名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "3687")
    @NotBlank(message = "能源名称不能为空")
    private String energyName;

    @Schema(hidden = true)
    @JsonIgnore
    private String energyCode;

    @Schema(description = "能源单位")
    private String energyUnit;

    @Schema(description = "计划用量")
    private BigDecimal plan;

}