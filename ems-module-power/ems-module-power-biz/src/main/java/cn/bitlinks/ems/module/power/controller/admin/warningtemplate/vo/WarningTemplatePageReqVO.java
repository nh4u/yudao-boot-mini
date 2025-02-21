package cn.bitlinks.ems.module.power.controller.admin.warningtemplate.vo;

import cn.bitlinks.ems.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.bitlinks.ems.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 告警模板分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class WarningTemplatePageReqVO extends PageParam {

    @Schema(description = "模板名称", example = "赵六")
    private String name;

    @Schema(description = "模板编码")
    private String code;

    @Schema(description = "模板标题/邮件标题")
    private String title;

    @Schema(description = "模板类型:0-站内信1-邮件", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Integer type;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}