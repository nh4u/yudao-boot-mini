package cn.bitlinks.ems.module.power.service.report.electricity;

import cn.bitlinks.ems.module.power.controller.admin.report.electricity.vo.*;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsResultV2VO;

import java.util.List;

/**
 * @author liumingqiang
 */
public interface ProductionConsumptionSettingsService {


    void updateBatch(List<ProductionConsumptionSettingsSaveReqVO> productionConsumptionList);

    List<ProductionConsumptionSettingsRespVO> getProductionConsumptionSettingsList(ProductionConsumptionSettingsPageReqVO pageReqVO);

    List<String> getName();

    StatisticsResultV2VO<ProductionConsumptionStatisticsInfo> productionConsumptionTable(ProductionConsumptionReportParamVO paramVO);


    List<List<String>> getExcelHeader(ProductionConsumptionReportParamVO paramVO);

    List<List<Object>> getExcelData(ProductionConsumptionReportParamVO paramVO);
}
