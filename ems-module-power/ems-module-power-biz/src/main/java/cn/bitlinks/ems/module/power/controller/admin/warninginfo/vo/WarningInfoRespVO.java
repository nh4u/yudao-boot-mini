package cn.bitlinks.ems.module.power.controller.admin.warninginfo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import com.alibaba.excel.annotation.*;

@Schema(description = "管理后台 - 告警信息 Response VO")
@Data
@ExcelIgnoreUnannotated
public class WarningInfoRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "29279")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "用户id", example = "30582")
    private Long userId;

    @Schema(description = "告警等级：紧急4 重要3 次要2 警告1 提示0", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("告警等级：紧急4 重要3 次要2 警告1 提示0")
    private Integer level;

    @Schema(description = "告警时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("告警时间")
    private LocalDateTime warningTime;

    @Schema(description = "处理状态:0-未处理1-处理中2-已处理", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @ExcelProperty("处理状态:0-未处理1-处理中2-已处理")
    private Integer status;

    @Schema(description = "设备名称与编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("设备名称与编号")
    private String deviceRel;

    @Schema(description = "模板id", requiredMode = Schema.RequiredMode.REQUIRED, example = "3996")
    @ExcelProperty("模板id")
    private Long templateId;

    @Schema(description = "策略id", requiredMode = Schema.RequiredMode.REQUIRED, example = "3996")
    @ExcelProperty("策略id")
    private Long strategyId;

    @Schema(description = "标题", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("标题")
    private String title;

    @Schema(description = "告警内容", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("告警内容")
    private String content;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}