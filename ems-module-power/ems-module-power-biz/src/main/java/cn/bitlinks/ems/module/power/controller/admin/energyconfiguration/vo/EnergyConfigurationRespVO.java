package cn.bitlinks.ems.module.power.controller.admin.energyconfiguration.vo;

import cn.bitlinks.ems.framework.excel.core.annotations.DictFormat;
import cn.bitlinks.ems.framework.excel.core.convert.DictConvert;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import java.math.BigDecimal;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import com.alibaba.excel.annotation.*;

import static cn.bitlinks.ems.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 能源配置 Response VO")
@Data
@ExcelIgnoreUnannotated
public class EnergyConfigurationRespVO {

    @Schema(description = "id", requiredMode = Schema.RequiredMode.REQUIRED, example = "14490")
    @ExcelProperty("id")
    private Long id;

    @Schema(description = "能源名称", example = "赵六")
    @ExcelProperty("能源名称")
    private String energyName;

    @Schema(description = "编码")
    @ExcelProperty("编码")
    private String code;

    @Schema(description = "能源分类 1：外购能源；2：园区能源")
    @ExcelProperty("能源分类")
    private Integer energyClassify;

    @Schema(description = "能源图标")
    @ExcelProperty("能源图标")
    private String energyIcon;

    @Schema(description = "能源参数")
    @ExcelProperty("能源参数")
    private String energyParameter;

    @Schema(description = "折标煤系数")
    @ExcelProperty("折标煤系数")
    private BigDecimal factor;

    @Schema(description = "折标煤公式")
    @ExcelProperty("折标煤公式")
    private String coalFormula;

    @Schema(description = "折标煤小数位数")
    @ExcelProperty("折标煤小数位数")
    private String coalScale;

    @Schema(description = "开始时间")
    @ExcelProperty("开始时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    @ExcelProperty("结束时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime endTime;

    @Schema(description = "计费方式")
    @ExcelProperty(value = "计费方式", converter = DictConvert.class)
    @DictFormat("billing_method") // TODO 代码优化：建议设置到对应的 DictTypeConstants 枚举类中
    private Integer billingMethod;

    @Schema(description = "单价详细", example = "11713")
    @ExcelProperty("单价详细")
    private String unitPrice;

    @Schema(description = "用能成本公式")
    @ExcelProperty("用能成本公式")
    private String unitPriceFormula;

    @Schema(description = "单价小数位")
    @ExcelProperty("单价小数位")
    private String unitPriceScale;

    @Schema(description = "创建时间")
    @ExcelProperty("创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime createTime;

    @Schema(description = "创建人")
    @ExcelProperty("创建人")
    private String creator;

}