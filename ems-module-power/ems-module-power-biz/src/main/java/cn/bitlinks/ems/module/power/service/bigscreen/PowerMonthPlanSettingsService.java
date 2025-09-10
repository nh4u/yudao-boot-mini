package cn.bitlinks.ems.module.power.service.bigscreen;

import cn.bitlinks.ems.module.power.controller.admin.bigscreen.vo.PowerMonthPlanSettingsPageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.bigscreen.vo.PowerMonthPlanSettingsSaveReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.bigscreen.PowerMonthPlanSettingsDO;

import java.util.List;

/**
 * @author liumingqiang
 */
public interface PowerMonthPlanSettingsService {


    void updateBatch(List<PowerMonthPlanSettingsSaveReqVO> powerMonthPlanSettingsList);

    List<PowerMonthPlanSettingsDO> getPowerMonthPlanSettingsList(PowerMonthPlanSettingsPageReqVO pageReqVO);
}
