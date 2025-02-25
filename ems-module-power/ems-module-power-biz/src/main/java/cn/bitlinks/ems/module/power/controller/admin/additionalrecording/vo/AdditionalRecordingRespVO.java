package cn.bitlinks.ems.module.power.controller.admin.additionalrecording.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import java.math.BigDecimal;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import com.alibaba.excel.annotation.*;
import cn.bitlinks.ems.framework.excel.core.annotations.DictFormat;
import cn.bitlinks.ems.framework.excel.core.convert.DictConvert;

@Schema(description = "管理后台 - 补录 Response VO")
@Data
@ExcelIgnoreUnannotated
public class AdditionalRecordingRespVO {

    @Schema(description = "id", requiredMode = Schema.RequiredMode.REQUIRED, example = "11841")
    @ExcelProperty("id")
    private Long id;

    @Schema(description = "凭证id", example = "4781")
    @ExcelProperty("凭证id")
    private Long voucherId;

    @Schema(description = "计量器具id", example = "21597")
    @ExcelProperty("计量器具id")
    private Long standingbookId;

    @Schema(description = "数值类型", example = "1")
    @ExcelProperty("数值类型")
    private String valueType;

    @Schema(description = "本次采集时间")
    @ExcelProperty("本次采集时间")
    private LocalDateTime thisCollectTime;

    @Schema(description = "本次数值")
    @ExcelProperty("本次数值")
    private BigDecimal thisValue;

    @Schema(description = "单位")
    @ExcelProperty("单位")
    private String unit;

    @Schema(description = "补录人")
    @ExcelProperty("补录人")
    private String recordPerson;

    @Schema(description = "补录原因", example = "不好")
    @ExcelProperty("补录原因")
    private String recordReason;

    @Schema(description = "补录方式")
    @ExcelProperty("补录方式")
    private Integer recordMethod;

    @Schema(description = "录入时间")
    @ExcelProperty("录入时间")
    private LocalDateTime enterTime;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;


}