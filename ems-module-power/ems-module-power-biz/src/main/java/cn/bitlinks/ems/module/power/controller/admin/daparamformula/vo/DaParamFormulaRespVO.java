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

    @Schema(description = "台账id", requiredMode = Schema.RequiredMode.REQUIRED, example = "13897")
    @ExcelProperty("台账id")
    private Long standingBookId;

    @Schema(description = "能源参数名称")
    @ExcelProperty("能源参数名称")
    private String energyParam;

    @Schema(description = "能源参数计算公式")
    @ExcelProperty("能源参数计算公式")
    private String energyFormula;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}