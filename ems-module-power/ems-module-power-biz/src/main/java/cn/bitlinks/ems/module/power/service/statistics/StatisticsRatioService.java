package cn.bitlinks.ems.module.power.service.statistics;

import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsParamVO;

import java.util.Map;

/**
 * 比值（同比、环比、定基比） Service 接口
 *
 * @author hero
 */
public interface StatisticsRatioService {
    Map<String, Object> standardCoalMomAnalysisTable(StatisticsParamVO paramVO);

    Map<String, Object> moneyMomAnalysisTable(StatisticsParamVO paramVO);

    Map<String, Object> utilizationRatioMomAnalysisTable(StatisticsParamVO paramVO);

    Object standardCoalMomAnalysisChart(StatisticsParamVO paramVO);

    Object moneyMomAnalysisChart(StatisticsParamVO paramVO);

    Object utilizationRatioMomAnalysisChart(StatisticsParamVO paramVO);

    Map<String, Object> standardCoalBenchmarkAnalysisTable(StatisticsParamVO paramVO);

    Map<String, Object> moneyBenchmarkAnalysisTable(StatisticsParamVO paramVO);

    Map<String, Object> utilizationRatioBenchmarkAnalysisTable(StatisticsParamVO paramVO);

    Object standardCoalBenchmarkAnalysisChart(StatisticsParamVO paramVO);

    Object moneyBenchmarkAnalysisChart(StatisticsParamVO paramVO);

    Object utilizationRatioBenchmarkAnalysisChart(StatisticsParamVO paramVO);
}