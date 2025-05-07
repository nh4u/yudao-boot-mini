package cn.bitlinks.ems.module.power.controller.admin.standingbook.acquisition.vo;

import lombok.*;
import io.swagger.v3.oas.annotations.media.Schema;
import cn.bitlinks.ems.framework.common.pojo.PageParam;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

import static cn.bitlinks.ems.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 台账-数采设置分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class StandingbookAcquisitionPageReqVO extends PageParam {

    @Schema(description = "设备数采启停开关（0：关；1开。）", example = "1")
    private Boolean status;

    @Schema(description = "台账id", example = "4669")
    private Long standingbookId;

    @Schema(description = "采集频率")
    private Long frequency;

    @Schema(description = "采集频率单位(秒、分钟、小时、天)")
    private Integer frequencyUnit;

    @Schema(description = "服务设置id", example = "22393")
    private Long serviceSettingsId;

    @Schema(description = "开始时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] startTime;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}