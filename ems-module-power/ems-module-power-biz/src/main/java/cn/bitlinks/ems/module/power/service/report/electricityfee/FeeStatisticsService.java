package cn.bitlinks.ems.module.power.service.report.electricityfee;

import cn.bitlinks.ems.module.power.controller.admin.report.supplyanalysis.vo.SupplyAnalysisReportParamVO;
import cn.bitlinks.ems.module.power.controller.admin.report.supplyanalysis.vo.SupplyAnalysisSettingsPageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.report.supplyanalysis.vo.SupplyAnalysisSettingsSaveReqVO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.*;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.SupplyAnalysisPieResultVO;
import cn.bitlinks.ems.module.power.dal.dataobject.report.supplyanalysis.SupplyAnalysisSettingsDO;

import java.util.List;

/**
 * @author liumingqiang
 */
public interface FeeStatisticsService {
    StatisticsResultV2VO<StatisticsInfoV2> feeStatisticsTable(StatisticsParamV2VO paramVO);

    StatisticsChartResultV2VO feeStatisticsChart(StatisticsParamV2VO paramVO);

    List<List<String>> getExcelHeader(StatisticsParamV2VO paramVO);

    List<List<Object>> getExcelData(StatisticsParamV2VO paramVO);
}
