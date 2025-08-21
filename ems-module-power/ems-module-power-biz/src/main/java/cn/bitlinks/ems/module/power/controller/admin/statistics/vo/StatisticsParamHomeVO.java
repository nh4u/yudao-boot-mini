package cn.bitlinks.ems.module.power.controller.admin.statistics.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.Arrays;

import static cn.bitlinks.ems.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

/**
 * @author wangl
 * @date 2025年05月24日 18:09
 */
@Schema(description = "管理后台 - 用能统计入参 VO")
@Data
@EqualsAndHashCode(callSuper = false)
public class StatisticsParamHomeVO {
    @Schema(description = "统计周期,最长不超1年", example = "[1734451200000, 1735315200000]")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    @Size(min = 2, max = 2, message = "统计周期不能为空")
    private LocalDateTime[] range;

    @Schema(description = "时间类型 0：日；1：月；2：年；3：时。")
    @NotNull(message = "时间类型不能为空")
    private Integer dateType;

    @Override
    public String toString() {
        return "StatisticsParamV2VO{" +
                "range=" + Arrays.toString(range) +
                ", dateType=" + dateType +
                '}';
    }
}
