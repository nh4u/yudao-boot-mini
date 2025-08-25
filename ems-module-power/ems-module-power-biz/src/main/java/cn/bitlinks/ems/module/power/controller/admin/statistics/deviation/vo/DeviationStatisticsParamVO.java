package cn.bitlinks.ems.module.power.controller.admin.statistics.deviation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

import static cn.bitlinks.ems.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

/**
 * LocalDateTime传值时要用时间戳
 * LocalDate传值时用2024-08-05
 *
 * @author liumingqiang
 */
@Schema(description = "管理后台 - 用能统计入参 VO")
@Data
@EqualsAndHashCode(callSuper = false)
public class DeviationStatisticsParamVO {

    @Schema(description = "统计周期,最长不超1年", example = "[\"2025-06-01 00:00:00\", \"2025-06-30 23:59:59\" ]")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    @Size(min = 2, max = 2, message = "统计周期不能为空")
    private LocalDateTime[] range;

    @Schema(description = "统计能源", example = "当能源ID不为空时，优先以能源ID统计")
    @NotNull(message = "能源不能为空")
    private Long energyId;


}