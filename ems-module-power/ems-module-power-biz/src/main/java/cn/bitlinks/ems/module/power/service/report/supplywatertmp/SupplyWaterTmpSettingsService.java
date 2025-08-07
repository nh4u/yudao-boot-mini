package cn.bitlinks.ems.module.power.service.report.supplywatertmp;

import cn.bitlinks.ems.module.power.controller.admin.report.supplyanalysis.vo.SupplyAnalysisReportParamVO;
import cn.bitlinks.ems.module.power.controller.admin.report.supplywatertmp.vo.SupplyWaterTmpReportParamVO;
import cn.bitlinks.ems.module.power.controller.admin.report.supplywatertmp.vo.SupplyWaterTmpSettingsPageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.report.supplywatertmp.vo.SupplyWaterTmpSettingsSaveReqVO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsResultV2VO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.SupplyAnalysisPieResultVO;
import cn.bitlinks.ems.module.power.dal.dataobject.report.supplywatertmp.SupplyWaterTmpSettingsDO;

import java.util.List;

/**
 * @author liumingqiang
 */
public interface SupplyWaterTmpSettingsService {


    void updateBatch(List<SupplyWaterTmpSettingsSaveReqVO> supplyAnalysisSettingsList);

    List<SupplyWaterTmpSettingsDO> getSupplyWaterTmpSettingsList(SupplyWaterTmpSettingsPageReqVO pageReqVO);

    List<String> getSystem();

    StatisticsResultV2VO supplyAnalysisTable(SupplyWaterTmpReportParamVO paramVO);

    SupplyAnalysisPieResultVO supplyAnalysisChart(SupplyWaterTmpReportParamVO paramVO);

    List<List<String>> getExcelHeader(SupplyWaterTmpReportParamVO paramVO);

    List<List<Object>> getExcelData(SupplyWaterTmpReportParamVO paramVO);
}
