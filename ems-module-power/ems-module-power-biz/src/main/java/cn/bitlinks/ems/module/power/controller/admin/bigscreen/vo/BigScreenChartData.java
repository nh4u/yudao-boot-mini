package cn.bitlinks.ems.module.power.controller.admin.bigscreen.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author liumingqiang
 */
@Schema(description = "管理后台 - 大屏图数据 VO")
@Data
public class BigScreenChartData {
    @Schema(description = "纯水/当期/8吋")
    private List<BigDecimal> y1;

    @Schema(description = "废水/上月同期/12吋")
    private List<BigDecimal> y2;

    @Schema(description = "x轴")
    private List<String> xdata;
}