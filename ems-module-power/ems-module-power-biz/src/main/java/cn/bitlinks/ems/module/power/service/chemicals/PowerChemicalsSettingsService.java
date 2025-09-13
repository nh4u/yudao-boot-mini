package cn.bitlinks.ems.module.power.service.chemicals;

import cn.bitlinks.ems.module.power.controller.admin.chemicals.vo.PowerChemicalsSettingsPageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.chemicals.vo.PowerChemicalsSettingsRespVO;
import cn.bitlinks.ems.module.power.controller.admin.chemicals.vo.PowerChemicalsSettingsSaveReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.chemicals.PowerChemicalsSettingsDO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author liumingqiang
 */
public interface PowerChemicalsSettingsService {


    void updateBatch(List<PowerChemicalsSettingsSaveReqVO> powerChemicalsSettingsList);

    List<PowerChemicalsSettingsDO> getPowerChemicalsSettingsList(PowerChemicalsSettingsPageReqVO pageReqVO);
    List<PowerChemicalsSettingsRespVO> getList(LocalDateTime startTime, LocalDateTime endTime);
}
