package cn.bitlinks.ems.module.power.controller.admin.report.hvac.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;


@Schema(description = "管理后台 -变压器利用率入参 VO")
@Data
@EqualsAndHashCode(callSuper = false)
public class TransformerUtilizationParamVO extends BaseTimeDateParamVO {

    @Schema(description = "变压器 选择")
    private List<Long> transformerIds;
}