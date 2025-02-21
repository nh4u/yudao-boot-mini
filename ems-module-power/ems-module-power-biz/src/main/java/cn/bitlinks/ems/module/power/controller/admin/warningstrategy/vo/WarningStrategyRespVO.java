package cn.bitlinks.ems.module.power.controller.admin.warningstrategy.vo;

import cn.bitlinks.ems.framework.common.enums.CommonStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import com.alibaba.excel.annotation.*;
import cn.bitlinks.ems.framework.excel.core.annotations.DictFormat;
import cn.bitlinks.ems.framework.excel.core.convert.DictConvert;

@Schema(description = "管理后台 - 告警策略 Response VO")
@Data
@ExcelIgnoreUnannotated
public class WarningStrategyRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "27747")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "规则名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "zzc")
    @ExcelProperty("规则名称")
    private String name;

    @Schema(description = "描述", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("描述")
    private String description;

    @Schema(description = "设备范围")
    @ExcelProperty("设备范围")
    private String deviceScope;

    @Schema(description = "设备分类范围")
    @ExcelProperty("设备分类范围")
    private String deviceTypeScope;

    @Schema(description = "告警条件", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("告警条件")
    private String condition;

    @Schema(description = "告警等级：紧急4 重要3 次要2 警告1 提示0", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty(value = "告警等级：紧急4 重要3 次要2 警告1 提示0", converter = DictConvert.class)
    @DictFormat("warning_level") // TODO 代码优化：建议设置到对应的 DictTypeConstants 枚举类中
    private Integer level;

    @Schema(description = "站内信模板id", requiredMode = Schema.RequiredMode.REQUIRED, example = "30741")
    @ExcelProperty("站内信模板id")
    private Long siteTemplateId;

    @Schema(description = "邮件模板id", example = "3143")
    @ExcelProperty("邮件模板id")
    private Long mailTemplateId;

    @Schema(description = "站内信人员", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("站内信人员")
    private String siteStaff;

    @Schema(description = "邮件人员")
    @ExcelProperty("邮件人员")
    private String mailStaff;

    @Schema(description = "公共人员")
    private String commonStaff;

    @Schema(description = "告警间隔", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("告警间隔")
    private String interval;

    @Schema(description = "告警间隔单位", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty(value = "告警间隔单位", converter = DictConvert.class)
    @DictFormat("warning_interval_unit") // TODO 代码优化：建议设置到对应的 DictTypeConstants 枚举类中
    private Integer intervalUnit;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

    @Schema(description = "创建人")
    private String creatorName;

    @Schema(description = "状态，参见 CommonStatusEnum 枚举", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer status;
}