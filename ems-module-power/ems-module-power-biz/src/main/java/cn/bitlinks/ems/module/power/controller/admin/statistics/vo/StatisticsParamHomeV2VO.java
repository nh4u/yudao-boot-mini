package cn.bitlinks.ems.module.power.controller.admin.statistics.vo;


import java.util.Arrays;

import javax.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author wangl
 * @date 2025年05月24日 18:09
 */
@Schema(description = "管理后台 - 用能统计入参 VO")
@Data
@EqualsAndHashCode(callSuper = false)
public class StatisticsParamHomeV2VO extends StatisticsParamHomeVO{

    @Schema(description = "能源类型 1：外购能源；2：园区能源。")
    @NotNull(message = "能源类型不能为空")
    private Integer energyClassify;
    @Override
    public String toString() {
        return "StatisticsParamHomeV2VO{" +
                "range=" + Arrays.toString(super.getRange()) +
                ", dateType=" + super.getDateType() +
                ", energyClassify=" + energyClassify +
                '}';
    }
}
