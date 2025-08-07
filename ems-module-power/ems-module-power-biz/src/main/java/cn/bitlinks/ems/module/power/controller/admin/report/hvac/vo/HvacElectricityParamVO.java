package cn.bitlinks.ems.module.power.controller.admin.report.hvac.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;


@Schema(description = "管理后台 - 暖通电力汇总报表入参 VO")
@Data
@EqualsAndHashCode(callSuper = false)
public class HvacElectricityParamVO extends BaseTimeDateParamVO {

    @Schema(description = "对应字典：report_hvac_electricity")
    private List<String> labelCodes;
}