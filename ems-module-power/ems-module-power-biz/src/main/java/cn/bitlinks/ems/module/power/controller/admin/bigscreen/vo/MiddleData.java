package cn.bitlinks.ems.module.power.controller.admin.bigscreen.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author liumingqiang
 */
@Schema(description = "管理后台 - 中部数据 VO")
@Data
@JsonInclude(JsonInclude.Include.ALWAYS)
public class MiddleData {

    @Schema(description = "今日能耗")
    private MiddleItemData todayConsumption;

    @Schema(description = "电力")
    private MiddleItemData power;

    @Schema(description = "RO水")
    private MiddleItemData roWater;

    @Schema(description = "UPW")
    private MiddleItemData upw;

    @Schema(description = "DIW")
    private MiddleItemData diw;

    @Schema(description = "氮（N）")
    private MiddleItemData nitrogen;

    @Schema(description = "氢（H）")
    private MiddleItemData hydrogen;

    @Schema(description = "氧（O）")
    private MiddleItemData oxygen;

    @Schema(description = "氩（Ar）")
    private MiddleItemData argon;

    @Schema(description = "氦（He）")
    private MiddleItemData helium;

    @Schema(description = "趋势图")
    private BigScreenChartData trendChart;

}