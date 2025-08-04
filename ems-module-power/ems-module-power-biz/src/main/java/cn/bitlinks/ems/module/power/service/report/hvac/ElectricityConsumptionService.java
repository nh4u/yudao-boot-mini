package cn.bitlinks.ems.module.power.service.report.hvac;

import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsChartPieResultVO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsParamV2VO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsResultV2VO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StructureInfo;

import java.util.List;

public interface ElectricityConsumptionService {
    /**
     * 表
     * @param paramVO
     * @return
     */
    StatisticsResultV2VO<StructureInfo> getTable(StatisticsParamV2VO paramVO);

    /**
     * 图表
     * @param paramVO
     * @return
     */
    StatisticsChartPieResultVO getChart(StatisticsParamV2VO paramVO);


    List<List<String>> getExcelHeader(StatisticsParamV2VO paramVO);

    List<List<Object>> getExcelData(StatisticsParamV2VO paramVO);
}
