package cn.bitlinks.ems.module.power.controller.admin.unitpricehistory.vo;

import cn.bitlinks.ems.framework.excel.core.annotations.DictFormat;
import cn.bitlinks.ems.framework.excel.core.convert.DictConvert;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import com.alibaba.excel.annotation.*;

import static cn.bitlinks.ems.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 单价历史 Response VO")
@Data
@ExcelIgnoreUnannotated
public class UnitPriceHistoryRespVO {

    @Schema(description = "id", requiredMode = Schema.RequiredMode.REQUIRED, example = "29507")
    @ExcelProperty("id")
    private Long id;

    @Schema(description = "能源id", example = "8300")
    @ExcelProperty("能源id")
    private Long energyId;

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

    @Schema(description = "核算频率")
    @ExcelProperty(value = "核算频率", converter = DictConvert.class)
    @DictFormat("accounting_frequency") // TODO 代码优化：建议设置到对应的 DictTypeConstants 枚举类中
    private Integer accountingFrequency;

    @Schema(description = "单价详细")
    @ExcelProperty("单价详细")
    private String priceDetails;

    @Schema(description = "计算公式")
    @ExcelProperty("计算公式")
    private String formula;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime createTime;

}