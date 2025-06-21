package cn.bitlinks.ems.module.power.service.copsettings;

import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.module.power.dal.dataobject.copsettings.CopFormulaDO;
import cn.bitlinks.ems.module.power.dal.dataobject.copsettings.CopSettingsDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.tmpl.StandingbookTmplDaqAttrDO;
import cn.bitlinks.ems.module.power.dal.mysql.copsettings.CopFormulaMapper;
import cn.bitlinks.ems.module.power.dal.mysql.copsettings.CopSettingsMapper;
import cn.bitlinks.ems.module.power.service.copsettings.dto.CopSettingsDTO;
import cn.bitlinks.ems.module.power.service.standingbook.tmpl.StandingbookTmplDaqAttrService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * cop报表设置 Service 接口
 *
 * @author bitlinks
 */
@Slf4j
@Service
@Validated
public class CopSettingsServiceImpl implements CopSettingsService {

    @Resource
    private CopSettingsMapper copSettingsMapper;
    @Resource
    private CopFormulaMapper copFormulaMapper;
    @Resource
    private StandingbookTmplDaqAttrService standingbookTmplDaqAttrService;

    @Override
    public List<CopFormulaDO> getCopFormulaList() {
        return copFormulaMapper.selectList();
    }

    @Override
    public List<CopSettingsDO> getCopSettingsList() {
        return copSettingsMapper.selectList();
    }

    @Override
    public List<CopSettingsDTO> getCopSettingsWithParamsList() {
        List<CopSettingsDTO> result = new ArrayList<>();
        // 1. 加载 COP 参数设置
        List<CopSettingsDO> settings = getCopSettingsList();
        // 2.cop依赖的所有台账id
        List<Long> standingbookIds = settings.stream()
                .map(CopSettingsDO::getStandingbookId)
                .collect(Collectors.toList());

        // 3.查询台账的所有的能源参数
        Map<Long, List<StandingbookTmplDaqAttrDO>> energyDaqAttrsBySbIdsMap = standingbookTmplDaqAttrService.getEnergyDaqAttrsBySbIds(standingbookIds);

        for (CopSettingsDO setting : settings) {
            Long sbId = setting.getStandingbookId();
            String cnName = setting.getParamCnName();

            List<StandingbookTmplDaqAttrDO> attrList = energyDaqAttrsBySbIdsMap.getOrDefault(sbId, Collections.emptyList());

            // 匹配参数中文名，获取对应编码
            Optional<StandingbookTmplDaqAttrDO> matched = attrList.stream()
                    .filter(attr -> cnName.equals(attr.getParameter()))
                    .findFirst();

            if (!matched.isPresent()) {
                log.info("未找到该COP参数【{}】中文名匹配的能源参数编码，跳过该COP", setting.getParam());
                continue;
            }
            String paramCode = matched.get().getCode();
            // 构造返回 DTO，可包含 copType、paramCode、paramCnName、sbId 等
            CopSettingsDTO dto = BeanUtils.toBean(setting, CopSettingsDTO.class);
            dto.setParamCode(paramCode);
            result.add(dto);
        }
        return result;

    }
}