package cn.bitlinks.ems.module.power.service.report.gas;

import cn.bitlinks.ems.module.power.controller.admin.report.electricity.vo.*;
import cn.bitlinks.ems.module.power.controller.admin.report.gas.vo.PowerTankSettingsRespVO;
import cn.bitlinks.ems.module.power.controller.admin.report.gas.vo.SettingsParamVO;

import java.util.List;

/**
 * 气化科报表 Service 接口
 *
 * @author bmqi
 */
public interface GasStatisticsService {

    List<PowerTankSettingsRespVO> getPowerTankSettings();

    Boolean savePowerTankSettings(SettingsParamVO paramVO);

    List<List<String>> getExcelHeader(ConsumptionStatisticsParamVO paramVO);

    List<List<Object>> getExcelData(ConsumptionStatisticsParamVO paramVO);
}