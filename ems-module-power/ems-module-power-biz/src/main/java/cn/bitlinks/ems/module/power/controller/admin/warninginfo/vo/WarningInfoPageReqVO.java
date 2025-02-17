package cn.bitlinks.ems.module.power.controller.admin.warninginfo.vo;

import lombok.*;
import java.util.*;
import io.swagger.v3.oas.annotations.media.Schema;
import cn.bitlinks.ems.framework.common.pojo.PageParam;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

import static cn.bitlinks.ems.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 告警信息分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class WarningInfoPageReqVO extends PageParam {

    @Schema(description = "告警等级：紧急4 重要3 次要2 警告1 提示0")
    private Integer level;

    @Schema(description = "告警时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] warningTime;

    @Schema(description = "处理状态:0-未处理1-处理中2-已处理", example = "2")
    private Integer status;

    @Schema(description = "设备名称与编号")
    private String deviceRel;

    @Schema(description = "告警内容")
    private String content;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}