package cn.bitlinks.ems.module.power.controller.admin.warningstrategy.vo;

import lombok.*;
import java.util.*;
import io.swagger.v3.oas.annotations.media.Schema;
import cn.bitlinks.ems.framework.common.pojo.PageParam;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

import static cn.bitlinks.ems.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 告警策略分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class WarningStrategyPageReqVO extends PageParam {

    @Schema(description = "规则名称", example = "zzc")
    private String name;

    @Schema(description = "告警等级：紧急4 重要3 次要2 警告1 提示0")
    private Integer level;

    @Schema(description = "创建人")
    private String creatorName;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}