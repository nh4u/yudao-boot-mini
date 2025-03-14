package cn.bitlinks.ems.module.power.service.statistics;

import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsParamVO;

import java.util.Map;

/**
 * 用能结构分析、价格结构分析 Service 接口
 *
 * @author hero
 */
public interface StatisticsStructureService {

    Map<String, Object> standardCoalStructureAnalysisTable(StatisticsParamVO paramVO);

    Object standardCoalStructureAnalysisChart(StatisticsParamVO paramVO);

    Map<String, Object> standardMoneyStructureAnalysisTable(StatisticsParamVO paramVO);

    Object standardMoneyStructureAnalysisChart(StatisticsParamVO paramVO);
}