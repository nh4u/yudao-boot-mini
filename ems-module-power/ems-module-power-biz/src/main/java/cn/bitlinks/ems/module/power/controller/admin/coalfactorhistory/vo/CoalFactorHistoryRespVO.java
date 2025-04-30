package cn.bitlinks.ems.module.power.controller.admin.coalfactorhistory.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import java.math.BigDecimal;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import com.alibaba.excel.annotation.*;

import static cn.bitlinks.ems.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 折标煤系数历史 Response VO")
@Data
@ExcelIgnoreUnannotated
public class CoalFactorHistoryRespVO {

    @Schema(description = "id", requiredMode = Schema.RequiredMode.REQUIRED, example = "11942")
    @ExcelProperty("id")
    private Long id;

    @Schema(description = "能源id", example = "31884")
    @ExcelProperty("能源id")
    private Long energyId;

    @Schema(description = "开始时间")
    @ExcelProperty("开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    @ExcelProperty("结束时间")
    private LocalDateTime endTime;

    @Schema(description = "折标煤系数")
    @ExcelProperty("折标煤系数")
    private BigDecimal factor;

    @Schema(description = "关联计算公式")
    @ExcelProperty("关联计算公式")
    private String formula;

    @Schema(description = "关联计算公式id")
    private Long formulaId;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime createTime;

    @Schema(description = "修改人")
    @ExcelProperty("修改人")
    private String updater;

    @Schema(description = "折标煤单位")
    @ExcelProperty("折标煤单位")
    private String unit;
}