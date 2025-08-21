package cn.bitlinks.ems.module.power.controller.admin.statistics.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "统计总览 单位能耗、转换率VO")
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
public class StatisticsHomeTop2Data {

    @Schema(description = "值")
    private BigDecimal value;

    @Schema(description = "数据更新时间", example = "数据更新时间")
    private LocalDateTime dataUpdateTime;



}
