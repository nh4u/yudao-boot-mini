package cn.bitlinks.ems.module.power.service.statistics;

import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsChartResultV2VO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsInfoV2;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsParamV2VO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsResultV2VO;

import java.util.List;

/**
 * 用能分析 Service 接口
 *
 * @author hero
 */
public interface StatisticsV2Service {

    /**
     * 用能成本分析
     * @param paramVO
     * @return
     */
    StatisticsResultV2VO<StatisticsInfoV2> moneyAnalysisTable(StatisticsParamV2VO paramVO);

    StatisticsChartResultV2VO moneyAnalysisChart(StatisticsParamV2VO paramVO);

    List<List<String>> getExcelHeader(StatisticsParamV2VO paramVO);

    List<List<Object>> getExcelData(StatisticsParamV2VO paramVO);
}