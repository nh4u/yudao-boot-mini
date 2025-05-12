package cn.bitlinks.ems.module.power.controller.admin.statistics.vo;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

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
@ToString(callSuper = true)
public class StatisticsParamV2VO {

    @Schema(description = "统计周期,最长不超1年", example = "[1734451200000, 1735315200000]")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    @Size(min = 2, max = 2, message = "统计周期不能为空")
    private LocalDateTime[] range;

    @Schema(description = "统计标签-下级标签（需去重）", example = "[1,2,3,4]")
    private String childLabels;

    @Schema(description = "统计标签-顶级标签", example = "[1,2,3,4]")
    private String topLabel;

    @Schema(description = "统计能源", example = "[1,2,3,4]")
    private List<Long> energyIds;

    @Schema(description = "查看类型 0：综合查看；1：按能源查看；2：按标签查看。 默认0")
    @NotNull(message = "查看类型不能为空")
    private Integer queryType;

    //
    @Schema(description = "时间类型 0：日；1：月；2：年；3：时。")
    @NotNull(message = "时间类型不能为空")
    private Integer dateType;

    @Schema(description = "能源类型 1：外购能源；2：园区能源。")
    @NotNull(message = "能源类型不能为空")
    private Integer energyClassify;


}