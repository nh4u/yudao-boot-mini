package cn.bitlinks.ems.module.power.controller.admin.report.gas.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * @author bmqi
 */
@Schema(description = "管理后台 - 储罐液位设置入参 VO")
@Data
@EqualsAndHashCode(callSuper = false)
public class SettingsParamVO {

    @Schema(description = "储罐液位设置数据list", example = "")
    private List<PowerTankSettingsParamVO> powerTankSettingsParamVOList;

}