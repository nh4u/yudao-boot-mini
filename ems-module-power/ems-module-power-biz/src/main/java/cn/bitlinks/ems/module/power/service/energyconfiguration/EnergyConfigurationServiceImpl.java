package cn.bitlinks.ems.module.power.service.energyconfiguration;

import cn.bitlinks.ems.framework.common.exception.ServiceException;
import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.module.power.controller.admin.energyconfiguration.vo.EnergyConfigurationPageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.energyconfiguration.vo.EnergyConfigurationRespVO;
import cn.bitlinks.ems.module.power.controller.admin.energyconfiguration.vo.EnergyConfigurationSaveReqVO;
import cn.bitlinks.ems.module.power.controller.admin.energyparameters.vo.EnergyParametersSaveReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.daparamformula.DaParamFormulaDO;
import cn.bitlinks.ems.module.power.dal.dataobject.energyconfiguration.EnergyConfigurationDO;
import cn.bitlinks.ems.module.power.dal.dataobject.energygroup.EnergyGroupDO;
import cn.bitlinks.ems.module.power.dal.dataobject.energyparameters.EnergyParametersDO;
import cn.bitlinks.ems.module.power.dal.dataobject.unitpriceconfiguration.UnitPriceConfigurationDO;
import cn.bitlinks.ems.module.power.dal.mysql.daparamformula.DaParamFormulaMapper;
import cn.bitlinks.ems.module.power.dal.mysql.energyconfiguration.EnergyConfigurationMapper;
import cn.bitlinks.ems.module.power.dal.mysql.energyparameters.EnergyParametersMapper;
import cn.bitlinks.ems.module.power.dal.mysql.standingbook.attribute.StandingbookAttributeMapper;
import cn.bitlinks.ems.module.power.dal.mysql.unitpriceconfiguration.UnitPriceConfigurationMapper;
import cn.bitlinks.ems.module.power.service.energygroup.EnergyGroupService;
import cn.bitlinks.ems.module.power.service.energyparameters.EnergyParametersService;
import cn.bitlinks.ems.module.power.service.standingbook.tmpl.StandingbookTmplDaqAttrService;
import cn.bitlinks.ems.module.power.service.unitpriceconfiguration.UnitPriceConfigurationService;
import cn.bitlinks.ems.module.system.api.user.AdminUserApi;
import cn.bitlinks.ems.module.system.api.user.dto.AdminUserRespDTO;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.nacos.common.utils.CollectionUtils;
import com.alibaba.nacos.common.utils.StringUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.bitlinks.ems.framework.security.core.util.SecurityFrameworkUtils.getLoginUserNickname;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.*;

/**
 * 能源配置 Service 实现类
 *
 * @author bitlinks
 */
@Service
@Validated
public class EnergyConfigurationServiceImpl implements EnergyConfigurationService {

    @Resource
    private EnergyConfigurationMapper energyConfigurationMapper;
    @Resource
    @Lazy
    private UnitPriceConfigurationService unitPriceConfigurationService;
    @Resource
    private UnitPriceConfigurationMapper unitPriceConfigurationMapper;
    @Resource
    private StandingbookAttributeMapper standingbookAttributeMapper;

    @Resource
    private DaParamFormulaMapper daParamFormulaMapper;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private EnergyParametersService energyParametersService;
    @Resource
    private EnergyParametersMapper energyParametersMapper;
    @Resource
    private EnergyGroupService energyGroupService;
    @Resource
    private StandingbookTmplDaqAttrService standingbookTmplDaqAttrService;

    @Override
    public Long createEnergyConfiguration(EnergyConfigurationSaveReqVO createReqVO) {
        //  检查能源编码是否重复
        checkEnergyCodeDuplicate(createReqVO.getCode(), null);
        //  子表编码查重（新增逻辑）
        checkEnergyParameterCodeDuplicate(createReqVO.getEnergyParameters());
        //  插入能源配置主表
        EnergyConfigurationDO energyConfiguration = BeanUtils.toBean(createReqVO, EnergyConfigurationDO.class);
        energyConfigurationMapper.insert(energyConfiguration);
        Long energyId = energyConfiguration.getId();

        //  插入能源参数子表
        if (CollectionUtils.isNotEmpty(createReqVO.getEnergyParameters())) {
            List<EnergyParametersSaveReqVO> params = createReqVO.getEnergyParameters().stream()
                    .peek(param -> param.setEnergyId(energyId)) // 设置能源ID
                    .collect(Collectors.toList());
            energyParametersService.batchCreateEnergyParameters(params);
        }

        return energyId;
    }

    @Override
    public void updateEnergyConfiguration(EnergyConfigurationSaveReqVO updateReqVO) {
        Long energyId = updateReqVO.getId();
        // 1. 校验主表存在性
        validateEnergyConfigurationExists(energyId);
        // 2. 检查能源编码重复性（排除自身）
        checkEnergyCodeDuplicate(updateReqVO.getCode(), energyId);
        // 2. 子表编码查重（新增逻辑）
        checkEnergyParameterCodeDuplicate(updateReqVO.getEnergyParameters());
        // 3. 处理单位变更校验（原有逻辑保留）
        String nickname = getLoginUserNickname();
        List<EnergyParametersSaveReqVO> newParameters = updateReqVO.getEnergyParameters();

        // 4. 更新主表记录
        EnergyConfigurationDO updateObj = BeanUtils.toBean(updateReqVO, EnergyConfigurationDO.class);
        updateObj.setUpdater(nickname);
        energyConfigurationMapper.updateById(updateObj);

        // 5. 处理子表参数（核心逻辑）
        handleEnergyParameters(energyId, newParameters);
    }

    private void checkEnergyParameterCodeDuplicate(List<EnergyParametersSaveReqVO> params) {
        if (CollUtil.isEmpty(params)) {
            return;
        }

        List<String> duplicateCodes = params.stream()
                .map(EnergyParametersSaveReqVO::getCode)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        if (CollUtil.isNotEmpty(duplicateCodes)) {
            throw exception(ENERGY_PARAMETER_CODE_DUPLICATE);
        }

        // --- 原有逻辑：检查数据库重复（更新后代码）---
        // 收集所有参数ID（包含新增参数的临时ID，如果有）
        Set<Long> excludeIds = params.stream()
                .map(EnergyParametersSaveReqVO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 提取所有待校验的 code
        List<String> codes = params.stream()
                .map(EnergyParametersSaveReqVO::getCode)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(codes)) {
            return;
        }

        // 查询是否存在重复 code（排除自身）
        Long count = energyParametersMapper.selectCount(
                Wrappers.<EnergyParametersDO>lambdaQuery()
                        .in(EnergyParametersDO::getCode, codes)
                        .notIn(CollUtil.isNotEmpty(excludeIds), EnergyParametersDO::getId, excludeIds)
                        .eq(EnergyParametersDO::getDeleted, 0)
        );
        if (count > 0) {
            throw exception(ENERGY_PARAMETER_CODE_DUPLICATE);
        }
    }

    private void handleEnergyParameters(Long energyId, List<EnergyParametersSaveReqVO> newParams) {
        if (newParams == null) {
            return;
        }

        // 1. 校验用量唯一性（原有逻辑）
        long usageCount = newParams.stream()
                .filter(p -> p.getUsage() != null && p.getUsage() == 1)
                .count();
        if (usageCount > 1) {
            throw exception(USAGE_MORE_THAN_ONE);
        }

        // 2. 获取数据库中现有参数
        List<EnergyParametersDO> oldParams = energyParametersMapper.selectList(
                Wrappers.<EnergyParametersDO>lambdaQuery()
                        .eq(EnergyParametersDO::getEnergyId, energyId)
        );

        // 3. 收集所有新参数的ID（排除新增的无ID参数）
        Set<Long> newParamIds = newParams.stream()
                .map(EnergyParametersSaveReqVO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 4. 标记待删除的旧参数（不在新参数ID列表中）
        List<EnergyParametersDO> toDelete = oldParams.stream()
                .filter(oldParam -> !newParamIds.contains(oldParam.getId()))
                .collect(Collectors.toList());

        // 5. 处理更新和新增参数
        List<EnergyParametersSaveReqVO> toUpdate = newParams.stream()
                .filter(p -> p.getId() != null)
                .peek(p -> p.setEnergyId(energyId))
                .collect(Collectors.toList());

        List<EnergyParametersSaveReqVO> toAdd = newParams.stream()
                .filter(p -> p.getId() == null)
                .peek(p -> p.setEnergyId(energyId))
                .collect(Collectors.toList());

        // 6.检查模板关联状态
        boolean isTemplateAssociated = standingbookTmplDaqAttrService.isAssociationWithEnergyId(energyId);
        if (isTemplateAssociated) {
            // 有关联模板时禁止删除和更新操作
            if (!toDelete.isEmpty() || !toUpdate.isEmpty()) {
                throw exception(ENERGY_CONFIGURATION_TEMPLATE_ASSOCIATED); // 使用新的异常码
            }
        }

        // 7. 执行删除、更新、新增
        if (!toDelete.isEmpty()) {
            List<Long> deleteIds = toDelete.stream()
                    .map(EnergyParametersDO::getId)
                    .collect(Collectors.toList());
            energyParametersMapper.deleteByIds(deleteIds);
        }
        if (!toUpdate.isEmpty()) {
            List<EnergyParametersDO> updateDOs = BeanUtils.toBean(toUpdate, EnergyParametersDO.class);
            energyParametersMapper.updateBatch(updateDOs);
        }
        if (!toAdd.isEmpty()) {
            List<EnergyParametersDO> addDOs = BeanUtils.toBean(toAdd, EnergyParametersDO.class);
            energyParametersMapper.insertBatch(addDOs);
            if (isTemplateAssociated) {
                standingbookTmplDaqAttrService.cascadeAddDaqAttrByEnergyParams(energyId, addDOs);
            }
        }
    }

    private String parseUnitFromJson(List<EnergyParametersSaveReqVO> parameters) {
        if (CollectionUtils.isEmpty(parameters)) {
            return "";
        }
        // 使用 Stream API 按新规则过滤
        return parameters.stream()
                // 过滤出标记为用量的参数（usage=1）
                .filter(param -> Integer.valueOf(1).equals(param.getUsage()))
                // 提取单位字段
                .map(EnergyParametersSaveReqVO::getUnit)
                // 过滤空值
                .filter(StrUtil::isNotBlank)
                // 取第一个符合条件的参数
                .findFirst()
                // 无结果返回空字符串
                .orElse("");
    }

    // 标准化 JSON 方法
    private String normalizeJson(String json) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(json);
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(node);
    }

    private void checkEnergyCodeDuplicate(String code, Long id) {
        if (StringUtils.isBlank(code)) {
            return; // 如果编码允许为空，根据业务需求调整
        }
        // 查询是否存在重复编码
        int count = energyConfigurationMapper.countByCodeAndNotId(code, id);
        if (count > 0) {
            throw exception(ENERGY_CODE_DUPLICATE);
        }
    }

    private void checkEnergyNameDuplicate(String energyName, Long id) {
        if (StringUtils.isBlank(energyName)) {
            return;
        }
        // 查询是否存在重复名称
        int count = energyConfigurationMapper.countByEnergyNameAndNotId(energyName, id);
        if (count > 0) {
            throw exception(ENERGY_NAME_DUPLICATE);
        }
    }

    @Override
    public void deleteEnergyConfiguration(Long id) {
        // 校验存在
        validateEnergyConfigurationExists(id);

        // 2. 检查是否关联模板
        boolean isTemplateAssociated = standingbookTmplDaqAttrService.isAssociationWithEnergyId(id);
        if (isTemplateAssociated) {
            throw exception(ENERGY_CONFIGURATION_TEMPLATE_ASSOCIATED);
        }

        // 3. 先删除子表（能源参数）
        energyParametersMapper.deleteByEnergyId(id); // 需要新增的Mapper方法

        // 4. 删除主表
        energyConfigurationMapper.deleteById(id);
    }

    @Override
    public void deleteEnergyConfigurations(List<Long> ids) {
        // 校验存在
        for (Long id : ids) {
            validateEnergyConfigurationExists(id);
        }

        // 2. 检查是否存在模板关联（任一配置关联模板则阻断删除）
        List<Long> invalidIds = ids.stream()
                .filter(id -> standingbookTmplDaqAttrService.isAssociationWithEnergyId(id))
                .collect(Collectors.toList());
        if (CollUtil.isNotEmpty(invalidIds)) {
            throw exception(ENERGY_CONFIGURATION_TEMPLATE_ASSOCIATED, invalidIds.get(0));
        }
        // 删除
        energyParametersMapper.deleteByEnergyIds(ids);
        energyConfigurationMapper.deleteByIds(ids);
    }

    private void validateEnergyConfigurationExists(Long id) {
        if (energyConfigurationMapper.selectById(id) == null) {
            throw exception(ENERGY_CONFIGURATION_NOT_EXISTS);
        }
    }

    @Override
    public EnergyConfigurationRespVO getEnergyConfiguration(Long id) {
        // 1. 查询主表
        EnergyConfigurationDO mainDO = energyConfigurationMapper.selectById(id);

        if (mainDO == null) {
            return null;
        }

        // 2. 查询子表（能源参数）
        List<EnergyParametersDO> parameters = energyParametersMapper.selectByEnergyId(id);

        // 3. 组装响应 VO
        EnergyConfigurationRespVO respVO = BeanUtils.toBean(mainDO, EnergyConfigurationRespVO.class);
        respVO.setEnergyParameters(parameters);
        return respVO;
    }

    @Override
    public PageResult<EnergyConfigurationRespVO> getEnergyConfigurationPage(EnergyConfigurationPageReqVO pageReqVO) {
        // 1. 查询主表分页数据
        PageResult<EnergyConfigurationDO> pageResult = energyConfigurationMapper.selectPage(pageReqVO);
        if (pageResult == null || CollectionUtils.isEmpty(pageResult.getList())) {
            return new PageResult<>(Collections.emptyList(), 0L);
        }

        // 2. 收集所有能源配置ID
        List<Long> energyIds = pageResult.getList().stream()
                .map(EnergyConfigurationDO::getId)
                .collect(Collectors.toList());

        // 3. 批量查询关联的能源参数（一次性查询）
        Map<Long, List<EnergyParametersDO>> paramsMap = energyParametersMapper.selectListByEnergyIds(energyIds)
                .stream()
                .collect(Collectors.groupingBy(EnergyParametersDO::getEnergyId));

        // 4. 转换VO并填充参数
        List<EnergyConfigurationRespVO> voList = pageResult.getList().stream().map(doObj -> {
            // 转换为VO
            EnergyConfigurationRespVO vo = BeanUtils.toBean(doObj, EnergyConfigurationRespVO.class);

            // 设置能源参数
            vo.setEnergyParameters(paramsMap.getOrDefault(doObj.getId(), Collections.emptyList()));

            // 原有逻辑：设置单价和创建人昵称
            LocalDateTime currentDateTime = LocalDateTime.now();
            UnitPriceConfigurationDO unitPriceConfigurationDO = unitPriceConfigurationService.getCurrentUnitConfigByEnergyId(doObj.getId());
            Long groupId = vo.getGroupId();
            EnergyGroupDO energyGroupDO = energyGroupService.getEnergyGroup(groupId);
            if (energyGroupDO != null) {
                String groupName = energyGroupDO.getName();
                vo.setGroupName(groupName);
            }
            vo.setUnitPrice(unitPriceConfigurationDO);
            vo.setBillingMethod(unitPriceConfigurationMapper.getBillingMethodByEnergyIdAndTime(doObj.getId(), currentDateTime));
            vo.setAccountingFrequency(unitPriceConfigurationMapper.getAccountingFrequencyByEnergyIdAndTime(doObj.getId(), currentDateTime));
            if (vo.getCreator() != null) {
                CommonResult<AdminUserRespDTO> user = adminUserApi.getUser(Long.valueOf(vo.getCreator()));
                vo.setCreator(user.getData() != null ? user.getData().getNickname() : vo.getCreator());
            }

            return vo;
        }).collect(Collectors.toList());

        // 5. 返回新的分页结果
        return new PageResult<>(voList, pageResult.getTotal());
    }

    @Override
    public List<EnergyConfigurationDO> getAllEnergyConfiguration(EnergyConfigurationSaveReqVO queryVO) {
        List<EnergyConfigurationDO> mainList = energyConfigurationMapper.selectList();
        if (CollectionUtils.isEmpty(mainList)) {
            return Collections.emptyList();
        }
        List<Long> energyIds = mainList.stream()
                .map(EnergyConfigurationDO::getId)
                .collect(Collectors.toList());
        Map<Long, List<EnergyParametersDO>> paramsMap = energyParametersMapper.selectListByEnergyIds(energyIds)
                .stream()
                .collect(Collectors.groupingBy(EnergyParametersDO::getEnergyId));
        return mainList.stream()
                .map(doObj -> {
                    EnergyConfigurationDO vo = BeanUtils.toBean(doObj, EnergyConfigurationDO.class);
                    vo.setEnergyParameters(paramsMap.getOrDefault(doObj.getId(), Collections.emptyList()));
                    return vo;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<EnergyConfigurationDO> getEnergyConfigurationList(EnergyConfigurationPageReqVO queryVO) {
        return energyConfigurationMapper.selectList(queryVO);
    }

    @Override
    public List<EnergyConfigurationRespVO> selectByCondition(String energyName, String energyClassify, String code) {
// 1. 构建查询条件
        QueryWrapper<EnergyConfigurationDO> queryWrapper = new QueryWrapper<>();
        if (StrUtil.isNotBlank(energyName)) {
            queryWrapper.eq("energy_name", energyName);
        }
        if (StrUtil.isNotBlank(energyClassify)) {
            queryWrapper.eq("energy_classify", energyClassify);
        }
        if (StrUtil.isNotBlank(code)) {
            queryWrapper.eq("code", code);
        }

        // 2. 查询主表数据
        List<EnergyConfigurationDO> mainList = energyConfigurationMapper.selectList(queryWrapper);
        if (CollectionUtils.isEmpty(mainList)) {
            return Collections.emptyList();
        }

        // 3. 批量查询关联参数
        List<Long> energyIds = mainList.stream()
                .map(EnergyConfigurationDO::getId)
                .collect(Collectors.toList());
        Map<Long, List<EnergyParametersDO>> paramsMap = energyParametersMapper.selectListByEnergyIds(energyIds)
                .stream()
                .collect(Collectors.groupingBy(EnergyParametersDO::getEnergyId));

        // 4. 组装 VO
        return mainList.stream()
                .map(doObj -> {
                    EnergyConfigurationRespVO vo = BeanUtils.toBean(doObj, EnergyConfigurationRespVO.class);
                    vo.setEnergyParameters(paramsMap.getOrDefault(doObj.getId(), Collections.emptyList()));
                    return vo;
                })
                .collect(Collectors.toList());
    }

    @Override
    public Map<Integer, List<EnergyConfigurationDO>> getEnergyMenu() {
        List<EnergyConfigurationDO> energyConfigurations = energyConfigurationMapper.selectList();
        // 把EnergyName->name
        energyConfigurations.forEach(e -> {
            e.setName(e.getEnergyName());
        });
        Map<Integer, List<EnergyConfigurationDO>> groupedByClassify = energyConfigurations.stream()
                .collect(Collectors.groupingBy(EnergyConfigurationDO::getEnergyClassify));
        return groupedByClassify;
    }

    @Override
    public void submitFormula(EnergyConfigurationSaveReqVO updateReqVO) {
        // 校验存在
        validateEnergyConfigurationExists(updateReqVO.getId());

        // 校验一下
        Integer formulaType = updateReqVO.getFormulaType();
        if (Objects.isNull(formulaType)) {
            throw exception(FORMULA_TYPE_NOT_EXISTS);
        }

        // TODO: 2025/1/18 对历史记录需要处理一下
        LocalDateTime now = LocalDateTime.now();
        DaParamFormulaDO daParamFormulaDO;
        if (formulaType == 1) {
            // 折标煤公式
            String coalScale = updateReqVO.getCoalScale();
            daParamFormulaDO = DaParamFormulaDO.builder()
                    .energyFormula(updateReqVO.getCoalFormula())
                    .energyId(updateReqVO.getId())
                    .startEffectiveTime(now)
                    .formulaScale(StrUtil.isNotEmpty(coalScale) ? Integer.valueOf(coalScale) : null)
                    .formulaType(formulaType).build();
        } else {
            // 用能成本公式
            String unitPriceScale = updateReqVO.getUnitPriceScale();
            daParamFormulaDO = DaParamFormulaDO.builder()
                    .energyFormula(updateReqVO.getUnitPriceFormula())
                    .energyId(updateReqVO.getId())
                    .startEffectiveTime(now)
                    .formulaScale(StrUtil.isNotEmpty(unitPriceScale) ? Integer.valueOf(unitPriceScale) : null)
                    .formulaType(formulaType).build();
        }

        // 先要更新对应的能源id   formulaType 的上一条数据更新一下结束时间
        DaParamFormulaDO latestOne = daParamFormulaMapper.getLatestOne(daParamFormulaDO);
        if (latestOne != null) {
            latestOne.setEndEffectiveTime(now);
            daParamFormulaMapper.updateById(latestOne);
        }

        // 插入数据
        daParamFormulaMapper.insert(daParamFormulaDO);

        // 更新
        EnergyConfigurationDO updateObj = BeanUtils.toBean(updateReqVO, EnergyConfigurationDO.class);
        energyConfigurationMapper.updateById(updateObj);
    }

    @Override
    public List<EnergyConfigurationDO> getEnergyTree() {
        Map<Integer, List<EnergyConfigurationDO>> energyMap = getEnergyMenu();

        List<EnergyConfigurationDO> list = new ArrayList<>();

        // 外购能源
        EnergyConfigurationDO energy1 = EnergyConfigurationDO.builder().name("外购能源").energyName("外购能源").id(2L).build();
        energy1.setChildren(energyMap.get(1));

        // 园区能源
        EnergyConfigurationDO energy2 = EnergyConfigurationDO.builder().name("园区能源").energyName("园区能源").id(3L).build();
        energy2.setChildren(energyMap.get(2));

        // 全部
        EnergyConfigurationDO parent = EnergyConfigurationDO.builder().name("全部").energyName("全部").id(1L).build();
        List<EnergyConfigurationDO> children = new ArrayList<>();
        children.add(energy1);
        children.add(energy2);
        parent.setChildren(children);

        list.add(parent);
        return list;
    }

    @Override
    public List<EnergyConfigurationDO> getByEnergyClassify(Set<Long> energyIds, Integer energyClassify) {
        if(CollectionUtil.isEmpty(energyIds) && Objects.isNull(energyClassify)){
            return Collections.emptyList();
        }
        LambdaQueryWrapper<EnergyConfigurationDO> wrapper = new LambdaQueryWrapper<>();
        if(CollectionUtil.isNotEmpty(energyIds)){
            wrapper.in(EnergyConfigurationDO::getId, energyIds);
            return energyConfigurationMapper.selectList(wrapper);
        }
        wrapper.eq(EnergyConfigurationDO::getEnergyClassify, energyClassify);
        return energyConfigurationMapper.selectList(wrapper);
    }
}