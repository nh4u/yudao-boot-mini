package cn.bitlinks.ems.module.power.controller.admin.statistics.vo;


import cn.bitlinks.ems.module.power.controller.admin.report.hvac.vo.BaseReportChartResultVO;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * @author liumingqiang
 */
@Schema(description = "首页-柱状图数据")
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@JsonInclude(JsonInclude.Include.ALWAYS)
public class StatisticsHomeBarVO<T> extends BaseReportChartResultVO<T> {

    /**
     * 平均值
     */
    @Schema(description = "平均值")
    private BigDecimal avg;

}
