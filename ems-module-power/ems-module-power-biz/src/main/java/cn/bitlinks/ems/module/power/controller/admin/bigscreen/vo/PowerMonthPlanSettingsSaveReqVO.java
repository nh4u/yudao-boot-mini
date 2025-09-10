package cn.bitlinks.ems.module.power.controller.admin.bigscreen.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Digits;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * @author liumingqiang
 */
@Schema(description = "管理后台 - 纯废水压缩空气设置新增/修改 Request VO")
@Data
public class PowerMonthPlanSettingsSaveReqVO {

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
    @Digits(integer = 20, fraction = 2)
    private BigDecimal plan;
}