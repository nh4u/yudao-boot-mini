package cn.bitlinks.ems.module.power.controller.admin.report.electricity.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

import static cn.bitlinks.ems.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

/**
 * LocalDateTime传值时要用时间戳
 * LocalDate传值时用2024-08-05
 *
 * @author liumingqiang
 */
@Schema(description = "管理后台 - 生产源耗入参 VO")
@Data
@EqualsAndHashCode(callSuper = false)
public class ProductionConsumptionReportParamVO {

    @Schema(description = "统计周期,最长不超1年", example = "[\"2025-06-23 00:00:00\", \"2025-06-29 00:00:00\" ]")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    @Size(min = 2, max = 2, message = "统计周期不能为空")
    private LocalDateTime[] range;

    @Schema(description = "系统")
    private List<String> nameList;

    @Schema(description = "时间类型 0：日；1：月；2：年；3：时。")
    @NotNull(message = "时间类型不能为空")
    private Integer dateType;




}