package cn.bitlinks.ems.module.power.controller.admin.statistics.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;

@Schema(description = "统计总览 折标煤用量统计/折价统计底层VO")
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
public class StatisticsHomeData {

    @Schema(description = "指标项名称，如：累计")
    private String item;

    @Schema(description = "当前值")
    private BigDecimal now;

    @Schema(description = "上期值")
    private BigDecimal previous;

    @Schema(description = "同比（%）")
    private BigDecimal YOY;

    @Schema(description = "环比（%）")
    private BigDecimal MOM;

}
