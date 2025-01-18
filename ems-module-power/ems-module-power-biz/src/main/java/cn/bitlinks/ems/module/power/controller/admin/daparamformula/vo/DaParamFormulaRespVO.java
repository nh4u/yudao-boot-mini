package cn.bitlinks.ems.module.power.controller.admin.daparamformula.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author liumingqiang
 */
@Schema(description = "管理后台 - 数据来源为关联计量器具时的参数公式 Response VO")
@Data
@ExcelIgnoreUnannotated
public class DaParamFormulaRespVO {

    @Schema(description = "id", requiredMode = Schema.RequiredMode.REQUIRED, example = "31342")
    @ExcelProperty("id")
    private Long id;

    @Schema(description = "能源id", requiredMode = Schema.RequiredMode.REQUIRED, example = "13897")
    @ExcelProperty("能源id")
    private Long energyId;

    @Schema(description = "能源参数名称")
    @ExcelProperty("能源参数名称")
    private String energyParam;

    @Schema(description = "能源参数计算公式")
    @ExcelProperty("能源参数计算公式")
    private String energyFormula;

    @Schema(description = "公式类型")
    @ExcelProperty("公式类型")
    private Integer formulaType;

    @Schema(description = "公式小数点")
    @ExcelProperty("公式小数点")
    private Integer formulaScale;

    @Schema(description = "开始生效时间")
    @ExcelProperty("开始生效时间")
    private LocalDateTime startEffectiveTime;

    @Schema(description = "结束生效时间")
    @ExcelProperty("结束生效时间")
    private LocalDateTime endEffectiveTime;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}