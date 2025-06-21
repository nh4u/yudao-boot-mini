package cn.bitlinks.ems.module.power.service.copsettings;

import cn.bitlinks.ems.module.power.dal.dataobject.copsettings.CopFormulaDO;
import cn.bitlinks.ems.module.power.dal.dataobject.copsettings.CopSettingsDO;
import cn.bitlinks.ems.module.power.service.copsettings.dto.CopSettingsDTO;

import java.util.List;

/**
 * cop报表设置 Service 接口
 *
 * @author bitlinks
 */
public interface CopSettingsService {

    /**
     * 获取cop报表设置列表
     * @return
     */
    List<CopSettingsDO> getCopSettingsList();
    /**
     * 获取cop报表设置列表
     * @return
     */
    List<CopFormulaDO> getCopFormulaList();


    /**
     * 获取cop报表设置列表
     * @return
     */
    List<CopSettingsDTO> getCopSettingsWithParamsList();
}