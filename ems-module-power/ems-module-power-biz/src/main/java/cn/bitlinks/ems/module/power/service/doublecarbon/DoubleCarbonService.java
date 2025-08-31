package cn.bitlinks.ems.module.power.service.doublecarbon;

import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.module.power.controller.admin.doublecarbon.vo.*;

import javax.validation.Valid;

public interface DoubleCarbonService {
    DoubleCarbonSettingsRespVO getSettings();

    void updSettings(DoubleCarbonSettingsUpdVO updVO);

    void updMapping(DoubleCarbonMappingUpdVO updVO);

    PageResult<DoubleCarbonMappingRespVO> getMappingPage(@Valid DoubleCarbonMappingPageReqVO pageReqVO);
}
