package cn.bitlinks.ems.module.power.service.statistics;

import java.util.List;

import cn.bitlinks.ems.module.power.controller.admin.statistics.StatisticsParamHomeV2VO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.ComparisonChartResultVO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsHomeResultVO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsOverviewEnergyData;

/**
 * 统计总览 Service 接口
 */
public interface StatisticsHomeService {

    /**
     * 统计总览接口
     *
     * @param paramVO
     * @return
     */
    StatisticsHomeResultVO overview(StatisticsParamHomeV2VO paramVO);

    ComparisonChartResultVO costChart(StatisticsParamHomeV2VO paramVO);

    ComparisonChartResultVO coalChart(StatisticsParamHomeV2VO paramVO);

    List<StatisticsOverviewEnergyData> energy(StatisticsParamHomeV2VO paramVO);
}
