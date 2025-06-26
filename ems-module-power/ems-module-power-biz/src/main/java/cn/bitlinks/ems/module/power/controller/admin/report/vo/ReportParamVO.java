package cn.bitlinks.ems.module.power.controller.admin.report.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

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
public class ReportParamVO {

    @Schema(description = "统计周期,最长不超1年", example = "[\"2025-06-23 10:17:00\", \"2025-06-29 10:17:00\" ]")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    @Size(min = 2, max = 2, message = "统计周期不能为空")
    private LocalDateTime[] range;

    @Schema(description = "低温冷机：LTC；低温系统：LTS；中温冷机：MTC；中温系统：MTS。", example = "[\"LTC\",\"LTS\"]")
    private List<String> copType;

    @Schema(description = "时间类型 0：日；1：月；2：年；3：时。")
    @NotNull(message = "时间类型不能为空")
    private Integer dateType;

    @Override
    public String toString() {
        return "StatisticsParamV2VO{" +
                "range=" + Arrays.toString(range) +
                ", copType=" + copType.toString() +
                ", dateType=" + dateType +
                '}';
    }
}