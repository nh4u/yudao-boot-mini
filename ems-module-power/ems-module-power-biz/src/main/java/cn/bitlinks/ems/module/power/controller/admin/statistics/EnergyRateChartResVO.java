package cn.bitlinks.ems.module.power.controller.admin.statistics;

import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.EnergyRateChartResultVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "图表统计 - 利用率转换率 图表返回结果")
@Data
public class EnergyRateChartResVO {
    @Schema(description = "图表列表")
    private List<EnergyRateChartResultVO<BigDecimal>> list;

    @Schema(description = "数据时间")
    private LocalDateTime dataTime;
}

