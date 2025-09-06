package cn.bitlinks.ems.module.power.service.bigscreen;

import cn.bitlinks.ems.module.power.controller.admin.bigscreen.vo.PowerPureWasteWaterGasSettingsPageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.bigscreen.vo.PowerPureWasteWaterGasSettingsSaveReqVO;
import cn.bitlinks.ems.module.power.controller.admin.report.supplywatertmp.vo.*;
import cn.bitlinks.ems.module.power.dal.dataobject.bigscreen.PowerPureWasteWaterGasSettingsDO;
import cn.bitlinks.ems.module.power.dal.dataobject.report.supplywatertmp.SupplyWaterTmpSettingsDO;

import java.util.List;

/**
 * @author liumingqiang
 */
public interface PowerPureWasteWaterGasSettingsService {


    void updateBatch(List<PowerPureWasteWaterGasSettingsSaveReqVO> powerPureWasteWaterGasSettingsList);

    List<PowerPureWasteWaterGasSettingsDO> getPowerPureWasteWaterGasSettingsList(PowerPureWasteWaterGasSettingsPageReqVO pageReqVO);
}
