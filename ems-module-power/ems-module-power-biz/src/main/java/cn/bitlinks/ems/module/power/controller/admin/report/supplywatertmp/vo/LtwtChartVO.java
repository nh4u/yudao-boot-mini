package cn.bitlinks.ems.module.power.controller.admin.report.supplywatertmp.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * @Title: ydme-ems
 * @description:
 * @Author: Mingqiang LIU
 * @Date 2025/08/12 16:34
 **/
@Data
public class LtwtChartVO {

    @Schema(description = "点位1")
    private List<BigDecimal> ydata1;

    @Schema(description = "点位2")
    private List<BigDecimal> ydata2;

    @Schema(description = "上限")
    private BigDecimal max;

    @Schema(description = "下限")
    private BigDecimal min;

    @Schema(description = "x轴")
    private List<String> xdata;

}
