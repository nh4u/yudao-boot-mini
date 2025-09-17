package cn.bitlinks.ems.module.power.service.doublecarbon;

import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.module.power.controller.admin.doublecarbon.vo.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.util.List;

public interface DoubleCarbonService {
    DoubleCarbonSettingsRespVO getSettings();

    void updSettings(DoubleCarbonSettingsUpdVO updVO);
    void updLastSyncTime(DoubleCarbonSettingsUpdVO updVO);

    void updMapping(DoubleCarbonMappingUpdVO updVO);

    PageResult<DoubleCarbonMappingRespVO> getMappingPage(@Valid DoubleCarbonMappingPageReqVO pageReqVO);

    void addMapping(String standingbookCode);
    void delMapping(List<String> standingbookCodes);

    List<DoubleCarbonMappingRespVO> getEffectiveMappings();

    DoubleCarbonMappingImportRespVO importExcel(MultipartFile file);
}
