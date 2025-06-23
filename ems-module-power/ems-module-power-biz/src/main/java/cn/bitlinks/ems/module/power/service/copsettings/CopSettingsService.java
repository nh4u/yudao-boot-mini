package cn.bitlinks.ems.module.power.service.copsettings;

import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.module.power.controller.admin.copsettings.vo.CopSettingsPageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.copsettings.vo.CopSettingsSaveReqVO;
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


    Long createCopSettings(CopSettingsSaveReqVO createReqVO);

    void updateCopSettings(CopSettingsSaveReqVO updateReqVO);

    void deleteCopSettings(Long id);

    CopSettingsDO getCopSettings(Long id);

    PageResult<CopSettingsDO> getCopSettingsPage(CopSettingsPageReqVO pageReqVO);

    List<CopSettingsDO> getCopSettingsListByCopType(CopSettingsPageReqVO pageReqVO);

    void updateBatch(List<CopSettingsSaveReqVO> copSettingsList);
}