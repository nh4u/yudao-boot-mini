package cn.bitlinks.ems.module.power.controller.admin.monitor.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.Size;
import java.time.LocalDateTime;

import static cn.bitlinks.ems.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 设备监控-告警 Response VO")
@Data
@ExcelIgnoreUnannotated
public class DeviceMonitorWarningReqVO {

    @Schema(description = "统计周期,最长不超1年", example = "[\"2025-06-23 10:17:00\", \"2025-06-29 10:17:00\" ]")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    @Size(min = 2, max = 2, message = "统计周期不能为空")
    private LocalDateTime[] range;

    @Schema(description = "设备id")
    private Long sbId;
}