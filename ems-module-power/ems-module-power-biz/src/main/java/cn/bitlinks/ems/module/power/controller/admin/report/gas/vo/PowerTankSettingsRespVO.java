package cn.bitlinks.ems.module.power.controller.admin.report.gas.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 储罐液位设置 Response VO")
@Data
@ExcelIgnoreUnannotated
public class PowerTankSettingsRespVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "29246")
    private Long id;

    @Schema(description = "储罐名称", example = "123")
    private String name;

    @Schema(description = "设备压差id", example = "123")
    private Long pressureDiffId;

    @Schema(description = "密度ρ", example = "1")
    private BigDecimal density;

    @Schema(description = "重力加速度g", example = "1")
    private BigDecimal gravityAcceleration;
}
