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
}