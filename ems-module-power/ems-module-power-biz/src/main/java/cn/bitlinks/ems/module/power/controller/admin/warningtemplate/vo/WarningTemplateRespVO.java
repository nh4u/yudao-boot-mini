package cn.bitlinks.ems.module.power.controller.admin.warningtemplate.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import com.alibaba.excel.annotation.*;

@Schema(description = "管理后台 - 告警模板 Response VO")
@Data
@ExcelIgnoreUnannotated
public class WarningTemplateRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "16957")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "模板名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "赵六")
    @ExcelProperty("模板名称")
    private String name;

    @Schema(description = "模板编码", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("模板编码")
    private String code;

    @Schema(description = "模板内容", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("模板内容")
    private String content;

    @Schema(description = "模板标题", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("模板标题")
    private String title;

    @Schema(description = "备注", example = "你猜")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "模板类型:0-站内信1-邮件", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @ExcelProperty("模板类型:0-站内信1-邮件")
    private Integer type;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}