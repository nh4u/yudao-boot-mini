package cn.bitlinks.ems.module.power.service.report.electricityfee;

import cn.bitlinks.ems.module.power.controller.admin.report.electricity.vo.FeeChartResultVO;
import cn.bitlinks.ems.module.power.controller.admin.report.electricity.vo.FeeChartYInfo;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsInfoV2;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsParamV2VO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsResultV2VO;

import java.util.List;

/**
 * @author liumingqiang
 */
public interface FeeStatisticsService {
    StatisticsResultV2VO<StatisticsInfoV2> feeStatisticsTable(StatisticsParamV2VO paramVO);

    FeeChartResultVO<FeeChartYInfo> feeStatisticsChart(StatisticsParamV2VO paramVO);

    List<List<String>> getExcelHeader(StatisticsParamV2VO paramVO);

    List<List<Object>> getExcelData(StatisticsParamV2VO paramVO);
}
