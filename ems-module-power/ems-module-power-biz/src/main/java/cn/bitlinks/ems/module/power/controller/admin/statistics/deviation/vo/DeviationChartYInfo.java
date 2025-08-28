package cn.bitlinks.ems.module.power.controller.admin.statistics.deviation.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;


/**
 * @author liumingqiang
 */
@Schema(description = "堆叠图Y轴数据")
@Data
@JsonInclude(JsonInclude.Include.ALWAYS)
public class DeviationChartYInfo {
    /**
     * 名称
     */
    @Schema(description = "名称")
    private String name;

    /**
     * y轴数据
     */
    @Schema(description = "y轴数据")
    private List<BigDecimal> data;
}
