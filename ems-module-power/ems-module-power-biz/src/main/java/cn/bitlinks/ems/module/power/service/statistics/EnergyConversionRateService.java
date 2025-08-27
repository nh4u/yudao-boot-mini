package cn.bitlinks.ems.module.power.service.statistics;

import cn.bitlinks.ems.module.power.controller.admin.statistics.EnergyRateChartResVO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.EnergyRateInfo;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsParamV2VO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsResultV2VO;

import java.util.List;

/**
 * 能源利用率
 */
public interface EnergyConversionRateService {
    StatisticsResultV2VO<EnergyRateInfo> getTable(StatisticsParamV2VO paramVO);


    EnergyRateChartResVO getChart(StatisticsParamV2VO paramVO);

    List<List<String>> getExcelHeader(StatisticsParamV2VO paramVO);

    List<List<Object>> getExcelData(StatisticsParamV2VO paramVO);
}
