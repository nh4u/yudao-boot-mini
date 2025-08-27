package cn.bitlinks.ems.module.power.controller.admin.report.hvac.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 基础查询条件，继承该类
 */
@Schema(description = "管理后台 - 利用率+ 转换率 入参 VO")
@Data
@EqualsAndHashCode(callSuper = false)
public class BaseTimeDateChartParamVO extends BaseTimeDateParamVO {

    @Schema(description = "能源类型 1：外购能源；2：园区能源。不传默认查询全部")
    private Integer energyClassify;

}