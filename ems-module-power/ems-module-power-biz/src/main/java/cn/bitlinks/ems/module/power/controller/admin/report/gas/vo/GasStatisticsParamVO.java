package cn.bitlinks.ems.module.power.controller.admin.report.gas.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

import static cn.bitlinks.ems.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

/**
 * LocalDateTime传值时要用时间戳
 * LocalDate传值时用2024-08-05
 *
 * @author bmqi
 */
@Schema(description = "管理后台 - 气化科报表入参 VO")
@Data
@EqualsAndHashCode(callSuper = false)
@ToString
public class GasStatisticsParamVO {

    @Schema(description = "统计周期,最长不超1年", example = "[\"2025-06-23 10:17:00\", \"2025-06-29 10:17:00\" ]")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    @Size(min = 2, max = 2, message = "统计周期不能为空")
    private LocalDateTime[] range;


    @Schema(description = "计量器具编码列表", example = "[\"LCDA-FAB1\", \"HCDA-FAB1\"]")
    private List<String> energyStatisticsItemCodes;

}