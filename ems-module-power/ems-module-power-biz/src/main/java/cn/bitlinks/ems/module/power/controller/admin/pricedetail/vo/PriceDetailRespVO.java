package cn.bitlinks.ems.module.power.controller.admin.pricedetail.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalTime;
import java.util.*;
import java.math.BigDecimal;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import com.alibaba.excel.annotation.*;

@Schema(description = "管理后台 - 单价详细 Response VO")
@Data
@ExcelIgnoreUnannotated
public class PriceDetailRespVO {

    @Schema(description = "id", requiredMode = Schema.RequiredMode.REQUIRED, example = "705")
    @ExcelProperty("id")
    private Long id;

    @Schema(description = "单价id", example = "9614")
    @ExcelProperty("单价id")
    private Long priceId;

    @Schema(description = "时段类型", example = "1")
    @ExcelProperty("时段类型")
    private Integer periodType;

    @Schema(description = "时段开始时间")
    @ExcelProperty("时段开始时间")
    private LocalTime periodStart;

    @Schema(description = "时段结束时间")
    @ExcelProperty("时段结束时间")
    private LocalTime periodEnd;

    @Schema(description = "档位用量下限")
    @ExcelProperty("档位用量下限")
    private BigDecimal usageMin;

    @Schema(description = "档位用量上限")
    @ExcelProperty("档位用量上限")
    private BigDecimal usageMax;

    @Schema(description = "单价", example = "4236")
    @ExcelProperty("单价")
    private BigDecimal unitPrice;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}