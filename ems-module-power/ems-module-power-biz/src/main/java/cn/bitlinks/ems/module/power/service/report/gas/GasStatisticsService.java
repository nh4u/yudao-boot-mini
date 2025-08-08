package cn.bitlinks.ems.module.power.service.report.gas;

import cn.bitlinks.ems.module.power.controller.admin.report.gas.vo.*;

import java.util.List;

/**
 * 气化科报表 Service 接口
 *
 * @author bmqi
 */
public interface GasStatisticsService {

    List<PowerTankSettingsRespVO> getPowerTankSettings();

    Boolean savePowerTankSettings(SettingsParamVO paramVO);

    List<EnergyStatisticsItemInfoRespVO> getEnergyStatisticsItems();

    GasStatisticsResultVO<GasStatisticsInfo> gasStatisticsTable(GasStatisticsParamVO paramVO);

    List<List<String>> getExcelHeader(GasStatisticsParamVO paramVO);

    List<List<Object>> getExcelData(GasStatisticsParamVO paramVO);
}