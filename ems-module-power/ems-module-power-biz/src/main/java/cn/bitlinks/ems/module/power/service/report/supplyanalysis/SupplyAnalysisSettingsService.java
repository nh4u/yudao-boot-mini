package cn.bitlinks.ems.module.power.service.report.supplyanalysis;

import cn.bitlinks.ems.module.power.controller.admin.report.supplyanalysis.vo.SupplyAnalysisPieResultVO;
import cn.bitlinks.ems.module.power.controller.admin.report.supplyanalysis.vo.SupplyAnalysisReportParamVO;
import cn.bitlinks.ems.module.power.controller.admin.report.supplyanalysis.vo.SupplyAnalysisSettingsPageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.report.supplyanalysis.vo.SupplyAnalysisSettingsSaveReqVO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsResultV2VO;
import cn.bitlinks.ems.module.power.dal.dataobject.report.supplyanalysis.SupplyAnalysisSettingsDO;

import java.util.List;

/**
 * @author liumingqiang
 */
public interface SupplyAnalysisSettingsService {


    void updateBatch(List<SupplyAnalysisSettingsSaveReqVO> supplyAnalysisSettingsList);

    List<SupplyAnalysisSettingsDO> getSupplyAnalysisSettingsList(SupplyAnalysisSettingsPageReqVO pageReqVO);

    List<String> getSystem();

    StatisticsResultV2VO supplyAnalysisTable(SupplyAnalysisReportParamVO paramVO);

    SupplyAnalysisPieResultVO supplyAnalysisChart(SupplyAnalysisReportParamVO paramVO);

    List<List<String>> getExcelHeader(SupplyAnalysisReportParamVO paramVO);

    List<List<Object>> getExcelData(SupplyAnalysisReportParamVO paramVO);
}
