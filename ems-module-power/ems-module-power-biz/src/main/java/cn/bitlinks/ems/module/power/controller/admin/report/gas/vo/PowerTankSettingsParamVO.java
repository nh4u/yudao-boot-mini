package cn.bitlinks.ems.module.power.controller.admin.report.gas.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * @author bmqi
 */
@Schema(description = "管理后台 - 储罐液位设置入参 VO")
@Data
@EqualsAndHashCode(callSuper = false)
public class PowerTankSettingsParamVO {

    private Long id;

    @Schema(description = "设备压差id", example = "123")
    private Long pressureDiffId;

    @Schema(description = "密度ρ", example = "1")
    private BigDecimal density;

    @Schema(description = "重力加速度g", example = "1")
    private BigDecimal gravityAcceleration;

    @Override
    public String toString() {
        return "PowerTankSettingsParamVO{" +
                "id=" + id +
                ", pressureDiffId=" + pressureDiffId +
                ", density=" + density +
                ", gravityAcceleration=" + gravityAcceleration +
                '}';
    }
}