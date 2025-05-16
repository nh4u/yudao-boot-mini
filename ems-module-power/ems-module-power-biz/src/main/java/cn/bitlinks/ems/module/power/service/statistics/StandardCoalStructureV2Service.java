package cn.bitlinks.ems.module.power.service.statistics;

import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StandardCoalInfo;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsChartResultV2VO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsParamV2VO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsResultV2VO;

/**
 * @Title: ydme-ems
 * @description:
 * @Author: Mingqiang LIU
 * @Date 2025/05/14 17:08
 **/
public interface StandardCoalStructureV2Service {
    /**
     * 折标煤表
     *
     * @param paramVO
     * @return
     */
    StatisticsResultV2VO<StandardCoalInfo> standardCoalStructureAnalysisTable(StatisticsParamV2VO paramVO);

    /**
     * 折标煤图
     *
     * @param paramVO
     * @return
     */
    StatisticsChartResultV2VO standardCoalStructureAnalysisChart(StatisticsParamV2VO paramVO);
}
