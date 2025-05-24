package cn.bitlinks.ems.module.power.service.statistics;

import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.ComparisonChartResultVO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsParamV2VO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsResultV2VO;

/**
 * 用能分析 同比分析 Service 接口
 *
 * @author hero
 */
public interface YoyV2Service {

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
