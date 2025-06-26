package cn.bitlinks.ems.module.power.service.copsettings;

import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.module.power.controller.admin.copsettings.vo.CopSettingsPageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.copsettings.vo.CopSettingsSaveReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.copsettings.CopFormulaDO;
import cn.bitlinks.ems.module.power.dal.dataobject.copsettings.CopSettingsDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.tmpl.StandingbookTmplDaqAttrDO;
import cn.bitlinks.ems.module.power.dal.mysql.copsettings.CopFormulaMapper;
import cn.bitlinks.ems.module.power.dal.mysql.copsettings.CopSettingsMapper;
import cn.bitlinks.ems.module.power.service.copsettings.dto.CopSettingsDTO;
import cn.bitlinks.ems.module.power.service.standingbook.tmpl.StandingbookTmplDaqAttrService;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.*;

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
                log.info("未找到该COP【{}】参数【{}】中文名匹配的能源参数编码，跳过该COP", setting.getCopType(),setting.getParam());
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

    @Override
    public Long createCopSettings(CopSettingsSaveReqVO createReqVO) {
        // 插入
        CopSettingsDO copSettingsDO = BeanUtils.toBean(createReqVO, CopSettingsDO.class);
        copSettingsMapper.insert(copSettingsDO);
        // 返回
        return copSettingsDO.getId();
    }

    @Override
    public void updateCopSettings(CopSettingsSaveReqVO updateReqVO) {

        validateCopSettingsExists(updateReqVO.getId());
        // 插入
        CopSettingsDO copSettingsDO = BeanUtils.toBean(updateReqVO, CopSettingsDO.class);
        copSettingsMapper.updateById(copSettingsDO);
    }

    @Override
    public void deleteCopSettings(Long id) {
        validateCopSettingsExists(id);
        copSettingsMapper.deleteById(id);
    }

    @Override
    public CopSettingsDO getCopSettings(Long id) {
        return copSettingsMapper.selectById(id);
    }

    @Override
    public PageResult<CopSettingsDO> getCopSettingsPage(CopSettingsPageReqVO pageReqVO) {
        return copSettingsMapper.selectPage(pageReqVO);
    }

    @Override
    public List<CopSettingsDO> getCopSettingsListByCopType(CopSettingsPageReqVO pageReqVO) {
        String copType = pageReqVO.getCopType();
        validateCopTypeExists(copType);

        return copSettingsMapper.getCopSettingsListByCopType(copType);

    }

    @Override
    public void updateBatch(List<CopSettingsSaveReqVO> copSettingsList) {

        // 校验
        if (CollectionUtil.isEmpty(copSettingsList)) {
            throw exception(COP_SETTINGS_LIST_NOT_EXISTS);
        }

        for (CopSettingsSaveReqVO copSettingsSaveReqVO : copSettingsList) {
            Long standingbookId = copSettingsSaveReqVO.getStandingbookId();
            if (Objects.isNull(standingbookId)) {
                throw exception(COP_SETTINGS_STANDINGbOOK_NOT_EMPTY);
            }
        }

        // 公式参数按copType分组 组内台账id不能重复 校验
        Map<String, List<CopSettingsSaveReqVO>> copTypeMap = copSettingsList.stream()
                .collect(Collectors.groupingBy(CopSettingsSaveReqVO::getCopType));

        // TODO: 2025/6/23 目前台账太少 需要先关闭重复校验，等联调完再放开
//        copTypeMap.forEach((k, v) -> {
//            List<Long> collect = v.stream()
//                    .map(CopSettingsSaveReqVO::getStandingbookId)
//                    .distinct()
//                    .collect(Collectors.toList());
//
//            if (collect.size() != v.size()) {
//                throw exception(STANDINGbOOK_REPEAT);
//            }
//        });

        // 统一保存
        List<CopSettingsDO> list = BeanUtils.toBean(copSettingsList, CopSettingsDO.class);
        copSettingsMapper.updateBatch(list);

    }

    private void validateCopSettingsExists(Long id) {
        if (copSettingsMapper.selectById(id) == null) {
            throw exception(COP_SETTINGS_NOT_EXISTS);
        }
    }

    private void validateCopTypeExists(String copType) {
        if (StrUtil.isEmpty(copType)) {
            throw exception(COP_SETTINGS_NOT_EXISTS);
        }
    }
}