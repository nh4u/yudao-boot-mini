package cn.bitlinks.ems.module.power.controller.admin.report.supplywatertmp.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Digits;
import javax.validation.constraints.NotEmpty;
import java.math.BigDecimal;

/**
 * @author liumingqiang
 */
@Schema(description = "管理后台 - 供应分析新增/修改 Request VO")
@Data
public class SupplyWaterTmpSettingsSaveReqVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "12042")
    private Long id;

    @Schema(description = "系统")
    @NotEmpty(message = "系统不能为空")
    private String system;

    @Schema(description = "台账id")
    private Long standingbookId;

    @Schema(description = "能源参数名称")
    private String energyParamName;

    @Schema(description = "上限")
    @Digits(integer = 3, fraction = 2, message = "上限整数位3，小数位2")
    private BigDecimal max;

    @Schema(description = "下限")
    @Digits(integer = 3, fraction = 2, message = "下限整数位3，小数位2")
    private BigDecimal min;

}