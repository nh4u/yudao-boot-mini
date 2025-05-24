package cn.bitlinks.ems.module.power.service.statistics;

import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.BaseStatisticsParamV2VO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.ComparisonChartResultVO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsParamV2VO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsResultV2VO;

/**
 * 用能分析 定基比分析 Service 接口
 *
 * @author hero
 */
public interface BaseV2Service {

    /**
     * 折价定基比分析-表
     * @param paramVO
     * @return
     */
    StatisticsResultV2VO discountAnalysisTable(BaseStatisticsParamV2VO paramVO);

    /**
     * 折价定基比分析-图
     * @param paramVO
     * @return
     */
    ComparisonChartResultVO discountAnalysisChart(BaseStatisticsParamV2VO paramVO);

    /**
     * 折煤定基比分析-表
     * @param paramVO
     * @return
     */
    StatisticsResultV2VO foldCoalAnalysisTable(BaseStatisticsParamV2VO paramVO);

    /**
     * 折煤定基比分析-图
     * @param paramVO
     * @return
     */
    ComparisonChartResultVO foldCoalAnalysisChart(BaseStatisticsParamV2VO paramVO);
}
