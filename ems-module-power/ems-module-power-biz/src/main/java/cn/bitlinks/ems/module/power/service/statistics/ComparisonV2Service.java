package cn.bitlinks.ems.module.power.service.statistics;

import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.*;

/**
 * 用能分析 环比分析 Service 接口
 *
 * @author hero
 */
public interface ComparisonV2Service {

    /**
     * 折价环比分析-表
     * @param paramVO
     * @return
     */
    StatisticsResultV2VO discountAnalysisTable(StatisticsParamV2VO paramVO);

    /**
     * 折价环比分析-图
     * @param paramVO
     * @return
     */
    ComparisonChartResultVO discountAnalysisChart(StatisticsParamV2VO paramVO);

    /**
     * 折煤环比分析-表
     * @param paramVO
     * @return
     */
    StatisticsResultV2VO foldCoalAnalysisTable(StatisticsParamV2VO paramVO);

    /**
     * 折煤环比分析-图
     * @param paramVO
     * @return
     */
    ComparisonChartResultVO foldCoalAnalysisChart(StatisticsParamV2VO paramVO);
}
