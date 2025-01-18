package cn.bitlinks.ems.module.power.controller.admin.daparamformula.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author liumingqiang
 */
@Schema(description = "管理后台 - 数据来源为关联计量器具时的参数公式历史记录新增/修改 Request VO")
@Data
public class DaParamFormulaSaveReqVO {

    @Schema(description = "id", requiredMode = Schema.RequiredMode.REQUIRED, example = "31342")
    private Long id;

    @Schema(description = "能源id", requiredMode = Schema.RequiredMode.REQUIRED, example = "13897")
    @NotNull(message = "能源id不能为空")
    private Long energyId;

    @Schema(description = "能源参数名称")
    private String energyParam;

    @Schema(description = "能源参数计算公式")
    private String energyFormula;

    @Schema(description = "公式类型")
    private Integer formulaType;

    @Schema(description = "公式小数点")
    private Integer formulaScale;

    @Schema(description = "生效时间")
    private String effectiveTime;
}