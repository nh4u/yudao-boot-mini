package cn.bitlinks.ems.module.power.controller.admin.statistics.vo;

import cn.bitlinks.ems.module.power.controller.admin.report.hvac.vo.BaseTimeDateParamVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;


/**
 * @author liumingqiang
 */
@Schema(description = "管理后台 - 用能统计入参（定基比专用） VO")
@Data
@EqualsAndHashCode(callSuper = false)
public class BaseMarkStatisticsParamVO extends BaseTimeDateParamVO {

    @Schema(description = "基准年限", example = "2025")
    @NotNull(message = "基准年限不能为空")
    private Integer benchmark;

}
