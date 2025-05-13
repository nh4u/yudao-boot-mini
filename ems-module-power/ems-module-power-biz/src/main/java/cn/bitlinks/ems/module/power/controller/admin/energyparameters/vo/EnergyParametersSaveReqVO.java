package cn.bitlinks.ems.module.power.controller.admin.energyparameters.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import javax.validation.constraints.*;

@Schema(description = "管理后台 - 能源参数新增/修改 Request VO")
@Data
public class EnergyParametersSaveReqVO {

    @Schema(description = "id", requiredMode = Schema.RequiredMode.REQUIRED, example = "26158")
    private Long id;

    @Schema(description = "能源id", example = "2682")
    private Long energyId;

    @Schema(description = "参数名称")
    private String parameter;

    @Schema(description = "编码")
    private String code;

    @Schema(description = "数据特征")
    private Integer dataFeature;

    @Schema(description = "单位")
    private String unit;

    @Schema(description = "数据类型")
    private Integer dataType;

    @Schema(description = "用量")
    private Integer usage;

}