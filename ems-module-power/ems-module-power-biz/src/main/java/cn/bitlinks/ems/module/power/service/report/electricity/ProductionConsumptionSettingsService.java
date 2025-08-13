package cn.bitlinks.ems.module.power.service.report.electricity;

import cn.bitlinks.ems.module.power.controller.admin.report.electricity.vo.ProductionConsumptionReportParamVO;
import cn.bitlinks.ems.module.power.controller.admin.report.electricity.vo.ProductionConsumptionSettingsPageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.report.electricity.vo.ProductionConsumptionSettingsSaveReqVO;
import cn.bitlinks.ems.module.power.controller.admin.report.electricity.vo.ProductionConsumptionStatisticsInfo;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsResultV2VO;
import cn.bitlinks.ems.module.power.dal.dataobject.report.electricity.ProductionConsumptionSettingsDO;

import java.util.List;

/**
 * @author liumingqiang
 */
public interface ProductionConsumptionSettingsService {


    void updateBatch(List<ProductionConsumptionSettingsSaveReqVO> productionConsumptionList);

    List<ProductionConsumptionSettingsDO> getProductionConsumptionSettingsList(ProductionConsumptionSettingsPageReqVO pageReqVO);

    List<String> getName();

    StatisticsResultV2VO<ProductionConsumptionStatisticsInfo> productionConsumptionTable(ProductionConsumptionReportParamVO paramVO);


    List<List<String>> getExcelHeader(ProductionConsumptionReportParamVO paramVO);

    List<List<Object>> getExcelData(ProductionConsumptionReportParamVO paramVO);
}
