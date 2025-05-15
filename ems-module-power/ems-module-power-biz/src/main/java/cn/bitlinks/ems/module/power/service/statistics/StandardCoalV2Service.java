package cn.bitlinks.ems.module.power.service.statistics;

import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.*;

/**
 * @Title: ydme-ems
 * @description:
 * @Author: Mingqiang LIU
 * @Date 2025/05/14 17:08
 **/
public interface StandardCoalV2Service {
    /**
     * 折标煤表
     *
     * @param paramVO
     * @return
     */
    StatisticsResultV2VO<StandardCoalInfo> standardCoalAnalysisTable(StatisticsParamV2VO paramVO);

    /**
     * 折标煤图
     *
     * @param paramVO
     * @return
     */
    StatisticsResultV2VO<StatisticsInfoV2> standardCoalAnalysisChart(StatisticsParamVO paramVO);
}
