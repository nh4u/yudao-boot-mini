package cn.bitlinks.ems.module.power.controller.admin.statistics.vo;

import cn.bitlinks.ems.module.power.controller.admin.report.hvac.vo.BaseReportChartResultVO;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@Schema(description = "管理后台 - 利用率+转换率 VO")
@JsonInclude(JsonInclude.Include.ALWAYS)
public class EnergyRateChartResultVO<T> extends BaseReportChartResultVO<T> {

    @Schema(description = "数据项", example = "热力")
    private String name;

}