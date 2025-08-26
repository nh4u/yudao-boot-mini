package cn.bitlinks.ems.module.power.service.statistics;

import cn.bitlinks.ems.module.power.controller.admin.report.hvac.vo.BaseReportChartResultVO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 能源利用率
 */
public interface EnergyUtilizationRateService {
    StatisticsResultV2VO<EnergyUtilizationRateInfo> getTable(StatisticsParamV2VO paramVO);

    /**
     * 折标煤图
     *
     * @param paramVO
     * @return
     */
    List<BaseReportChartResultVO<BigDecimal>> getChart(StatisticsParamV2VO paramVO);

}
