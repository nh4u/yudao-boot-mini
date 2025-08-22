package cn.bitlinks.ems.module.power.controller.admin.statistics.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;


@Schema(description = "管理后台 - 首页返回结果 VO")
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
public class StatisticsHomeResultVO {



    @Schema(description = "能源列表数据", example = "能源列表数据")
    private List<StatisticsOverviewEnergyData> statisticsOverviewEnergyDataList;

    @Schema(description = "折标煤用量统计", example = "折标煤用量统计", hidden = true)
    private List<StatisticsHomeData> standardCoalStatistics;

    @Schema(description = "折价统计", example = "折价统计", hidden = true)
    private List<StatisticsHomeData> moneyStatistics;

    @Schema(description = "数据更新时间", example = "数据更新时间")
    private LocalDateTime dataUpdateTime;
}
