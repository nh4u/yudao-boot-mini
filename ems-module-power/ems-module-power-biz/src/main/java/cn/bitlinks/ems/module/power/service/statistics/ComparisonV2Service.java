package cn.bitlinks.ems.module.power.service.statistics;

import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsParamV2VO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsResultV2VO;

/**
 * 用能分析 环比分析 Service 接口
 *
 * @author hero
 */
public interface ComparisonV2Service {

    /**
     * 折价环比分析
     * @param paramVO
     * @return
     */
    StatisticsResultV2VO discountAnalysisTable(StatisticsParamV2VO paramVO);
}
