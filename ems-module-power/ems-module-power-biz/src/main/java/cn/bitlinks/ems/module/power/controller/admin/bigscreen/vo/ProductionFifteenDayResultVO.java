package cn.bitlinks.ems.module.power.controller.admin.bigscreen.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * @author liumingqiang
 */
@Schema(description = "统计总览 能源消耗 VO")
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
@JsonInclude(JsonInclude.Include.ALWAYS)
public class ProductionFifteenDayResultVO extends BigScreenChartData {

    @Schema(description = "今日（8吋）")
    private BigDecimal today8;

    @Schema(description = "今日（12吋）")
    private BigDecimal today12;


//    @Schema(description = "X轴")
//    private List<String> x;
//
//    @Schema(description = "8吋")
//    private List<BigDecimal> production8;
//
//    @Schema(description = "12吋")
//    private List<BigDecimal> production12;

}