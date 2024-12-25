package cn.bitlinks.ems.module.power.service.statistics;

import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsParamVO;
import com.alibaba.fastjson.JSONObject;

import java.util.Map;

/**
 * 用能分析 Service 接口
 *
 * @author hero
 */
public interface StatisticsService {

    /**
     * 能留分析图
     *
     * @param paramVO 入参
     * @return 数据
     */
    Map<String, Object> energyFlowAnalysis(StatisticsParamVO paramVO);

    Map<String, Object> moneyAnalysisTable(StatisticsParamVO paramVO);

    Object  moneyAnalysisChart(StatisticsParamVO paramVO);

    Map<String, Object> standardCoalAnalysisTable(StatisticsParamVO paramVO);

    Object  standardCoalAnalysisChart(StatisticsParamVO paramVO);
}