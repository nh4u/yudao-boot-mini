package cn.bitlinks.ems.module.power.controller.admin.report.electricity.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;


/**
 * @author bmqi
 */
@Schema(description = "堆叠图Y轴数据")
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConsumptionStatisticsChartYInfo {
    /**
     * 元素名称
     */
    @Schema(description = "名称")
    private String name;

    /**
     * 对应的数据
     */
    @Schema(description = "y数据")
    private List<BigDecimal> data;
}
