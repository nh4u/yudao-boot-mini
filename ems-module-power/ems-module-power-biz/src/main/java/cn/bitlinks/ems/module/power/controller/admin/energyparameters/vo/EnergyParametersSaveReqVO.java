package cn.bitlinks.ems.module.power.controller.admin.energyparameters.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import javax.validation.constraints.*;

@Schema(description = "管理后台 - 能源参数新增/修改 Request VO")
@Data
public class EnergyParametersSaveReqVO {

    @Schema(description = "id", requiredMode = Schema.RequiredMode.REQUIRED, example = "5111")
    private Long id;

    @Schema(description = "能源id", example = "10576")
    private Long energyId;

    @Schema(description = "中文名")
    private String chinese;

    @Schema(description = "编码")
    private String code;

    @Schema(description = "数据特征值")
    private Integer characteristic;

    @Schema(description = "单位")
    private String unit;

    @Schema(description = "数据类型", example = "2")
    private Integer type;

    @Schema(description = "对应数采参数")
    private String acquisition;

}