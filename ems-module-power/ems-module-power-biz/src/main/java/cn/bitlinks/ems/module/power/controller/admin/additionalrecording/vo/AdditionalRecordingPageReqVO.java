package cn.bitlinks.ems.module.power.controller.admin.additionalrecording.vo;

import lombok.*;
import java.util.*;
import io.swagger.v3.oas.annotations.media.Schema;
import cn.bitlinks.ems.framework.common.pojo.PageParam;
import java.math.BigDecimal;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

import static cn.bitlinks.ems.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 补录分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AdditionalRecordingPageReqVO extends PageParam {

    @Schema(description = "凭证id", example = "4781")
    private Long voucherId;

    @Schema(description = "计量器具id", example = "21597")
    private Long standingbookId;

    @Schema(description = "数值类型", example = "1")
    private String valueType;

    @Schema(description = "本次采集时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] thisCollectTime;

    @Schema(description = "本次数值")
    private BigDecimal thisValue;

    @Schema(description = "单位")
    private String unit;

    @Schema(description = "补录人")
    private String recordPerson;

    @Schema(description = "补录原因", example = "不好")
    private String recordReason;

    @Schema(description = "补录方式")
    private Integer recordMethod;

    @Schema(description = "录入时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] enterTime;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}