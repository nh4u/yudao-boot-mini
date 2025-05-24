package cn.bitlinks.ems.module.power.service.statistics;

import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.ComparisonChartResultVO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsHomeResultVO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsParamV2VO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsParamVO;

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
    StatisticsHomeResultVO overview(StatisticsParamV2VO paramVO);

    ComparisonChartResultVO costChart(StatisticsParamV2VO paramVO);

    ComparisonChartResultVO coalChart(StatisticsParamV2VO paramVO);
}
