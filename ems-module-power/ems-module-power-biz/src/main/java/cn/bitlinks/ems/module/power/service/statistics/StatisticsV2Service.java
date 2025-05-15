package cn.bitlinks.ems.module.power.service.statistics;

import java.io.IOException;
import java.util.Map;

import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsBarVO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsChartResultV2VO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsParamV2VO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsParamVO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsResultV2VO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsResultVO;

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
    StatisticsResultV2VO moneyAnalysisTable(StatisticsParamV2VO paramVO);

    StatisticsChartResultV2VO moneyAnalysisChart(StatisticsParamV2VO paramVO);
}