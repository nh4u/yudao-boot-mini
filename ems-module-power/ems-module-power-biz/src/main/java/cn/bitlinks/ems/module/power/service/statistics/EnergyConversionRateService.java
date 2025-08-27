package cn.bitlinks.ems.module.power.service.statistics;

import cn.bitlinks.ems.module.power.controller.admin.report.hvac.vo.BaseReportChartResultVO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.EnergyRateInfo;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsParamV2VO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsResultV2VO;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.List;

/**
 * 能源利用率
 */
public interface EnergyConversionRateService {
    StatisticsResultV2VO<EnergyRateInfo> getTable(@Valid StatisticsParamV2VO paramVO);


    List<BaseReportChartResultVO<BigDecimal>> getChart(@Valid StatisticsParamV2VO paramVO);

    List<List<String>> getExcelHeader(@Valid StatisticsParamV2VO paramVO);

    List<List<Object>> getExcelData(@Valid StatisticsParamV2VO paramVO);
}
