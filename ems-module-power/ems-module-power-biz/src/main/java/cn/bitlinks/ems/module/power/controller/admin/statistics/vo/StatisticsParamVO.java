package cn.bitlinks.ems.module.power.controller.admin.statistics.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

import static cn.bitlinks.ems.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

/**
 * @author liumingqiang
 */
@Schema(description = "管理后台 - 用能统计入参 VO")
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
public class StatisticsParamVO {

    @Schema(description = "统计周期",example = "[2024-08-01, 2024-08-05]")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate[] range;

    @Schema(description = "统计标签（需去重）", example = "[1,2,3,4]")
    private List<Long> labelIds;

    @Schema(description = "统计能源", example = "[1,2,3,4]")
    private List<Long> energyIds;

    @Schema(description = "查看类型 0：综合查看；1：按能源查看；2：按标签查看。 默认0")
    private Integer queryType = 0;

    @Schema(description = "时间类型 0：日；1：月；2：年。 默认0")
    private Integer dateType = 0;

}