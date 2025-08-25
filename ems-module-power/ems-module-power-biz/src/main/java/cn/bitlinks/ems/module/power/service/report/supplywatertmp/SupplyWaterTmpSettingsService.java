package cn.bitlinks.ems.module.power.service.report.supplywatertmp;

import cn.bitlinks.ems.module.power.controller.admin.report.supplywatertmp.vo.*;
import cn.bitlinks.ems.module.power.dal.dataobject.report.supplywatertmp.SupplyWaterTmpSettingsDO;

import java.util.List;

/**
 * @author liumingqiang
 */
public interface SupplyWaterTmpSettingsService {


    void updateBatch(List<SupplyWaterTmpSettingsSaveReqVO> supplyWaterTmpSettingsList);

    List<SupplyWaterTmpSettingsDO> getSupplyWaterTmpSettingsList(SupplyWaterTmpSettingsPageReqVO pageReqVO);

    List<SupplyWaterTmpSettingsDO> getSystem();

    SupplyWaterTmpTableResultVO supplyWaterTmpTable(SupplyWaterTmpReportParamVO paramVO);

    SupplyWaterTmpChartResultVO supplyWaterTmpChart(SupplyWaterTmpReportParamVO paramVO);

    List<List<String>> getExcelHeader(SupplyWaterTmpReportParamVO paramVO);

    List<List<Object>> getExcelData(SupplyWaterTmpReportParamVO paramVO);
}
