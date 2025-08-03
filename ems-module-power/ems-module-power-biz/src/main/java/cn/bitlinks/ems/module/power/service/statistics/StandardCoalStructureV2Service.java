package cn.bitlinks.ems.module.power.service.statistics;

import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.*;

import java.util.List;

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
    StatisticsResultV2VO<StructureInfo> standardCoalStructureAnalysisTable(StatisticsParamV2VO paramVO);

    /**
     * 折标煤图
     *
     * @param paramVO
     * @return
     */
    StatisticsChartPieResultVO standardCoalStructureAnalysisChart(StatisticsParamV2VO paramVO);

    List<List<String>> getExcelHeader(StatisticsParamV2VO paramVO);

    List<List<Object>> getExcelData(StatisticsParamV2VO paramVO);
}
