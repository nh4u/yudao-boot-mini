package cn.bitlinks.ems.module.power.controller.admin.minitor.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author liumingqiang
 */
@Schema(description = "管理后台 - 台账属性 Response VO")
@Data
@JsonInclude(JsonInclude.Include.ALWAYS)
public class MinitorDetailRespVO {

    @Schema(description = "表-数据")
    List<MinitorDetailData> table;

    @Schema(description = "图-数据")
    List<BigDecimal> chartData;

    @Schema(description = "图-x轴")
    List<String> chartX;

    @Schema(description = "平均值")
    private BigDecimal avg;

    @Schema(description = "最大值")
    private BigDecimal max;

    @Schema(description = "最小值")
    private BigDecimal min;

}
