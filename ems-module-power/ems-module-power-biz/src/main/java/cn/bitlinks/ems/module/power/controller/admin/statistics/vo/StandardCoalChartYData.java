package cn.bitlinks.ems.module.power.controller.admin.statistics.vo;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * @author wangl
 * @date 2025年05月14日 15:18
 */
@Schema(description = "堆叠图Y轴数据")
@Data
public class StandardCoalChartYData {

    private BigDecimal max;
    private BigDecimal min;
    private BigDecimal standardCoal;
    private BigDecimal avg;
}
