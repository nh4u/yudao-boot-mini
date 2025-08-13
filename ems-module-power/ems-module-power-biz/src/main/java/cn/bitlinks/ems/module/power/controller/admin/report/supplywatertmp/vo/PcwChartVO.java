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
public class PcwChartVO {

    @Schema(description = "点位1：锅炉供水温度/供水压力")
    private List<BigDecimal> ydata11;

    @Schema(description = "点位1：市政供水温度/供水温度")
    private List<BigDecimal> ydata12;

    @Schema(description = "点位2：锅炉供水温度/供水压力")
    private List<BigDecimal> ydata21;

    @Schema(description = "点位2：市政供水温度/供水温度")
    private List<BigDecimal> ydata22;

    @Schema(description = "锅炉上限/压力上限")
    private Integer max1;

    @Schema(description = "锅炉下限/压力下限")
    private Integer min1;

    @Schema(description = "市政上限/温度上限")
    private Integer max2;

    @Schema(description = "市政下限/温度下限")
    private Integer min2;

    @Schema(description = "x轴")
    private List<String> xdata;

}
