package cn.bitlinks.ems.module.power.service.statistics;

import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsChartPieResultVO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsParamV2VO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsResultV2VO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StructureInfo;

/**
 * @Title: ydme-ems
 * @description:
 * @Author: Mingqiang LIU
 * @Date 2025/05/14 17:08
 **/
public interface MoneyStructureV2Service {
    /**
     * 成本结构表
     *
     * @param paramVO
     * @return
     */
    StatisticsResultV2VO<StructureInfo> moneyStructureAnalysisTable(StatisticsParamV2VO paramVO);

    /**
     * 成本结构图
     *
     * @param paramVO
     * @return
     */
    StatisticsChartPieResultVO moneyStructureAnalysisChart(StatisticsParamV2VO paramVO);

}
