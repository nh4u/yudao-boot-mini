package cn.bitlinks.ems.module.power.service.energyconfiguration;

import cn.bitlinks.ems.framework.common.exception.ServiceException;
import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.module.power.controller.admin.energyconfiguration.vo.EnergyConfigurationPageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.energyconfiguration.vo.EnergyConfigurationSaveReqVO;
import cn.bitlinks.ems.module.power.controller.admin.energyparameters.vo.EnergyParametersSaveReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.daparamformula.DaParamFormulaDO;
import cn.bitlinks.ems.module.power.dal.dataobject.energyconfiguration.EnergyConfigurationDO;
import cn.bitlinks.ems.module.power.dal.dataobject.energyparameters.EnergyParametersDO;
import cn.bitlinks.ems.module.power.dal.mysql.daparamformula.DaParamFormulaMapper;
import cn.bitlinks.ems.module.power.dal.mysql.energyconfiguration.EnergyConfigurationMapper;
import cn.bitlinks.ems.module.power.dal.mysql.energyparameters.EnergyParametersMapper;
import cn.bitlinks.ems.module.power.dal.mysql.standingbook.attribute.StandingbookAttributeMapper;
import cn.bitlinks.ems.module.power.dal.mysql.unitpriceconfiguration.UnitPriceConfigurationMapper;
import cn.bitlinks.ems.module.power.service.energyparameters.EnergyParametersService;
import cn.bitlinks.ems.module.system.api.user.AdminUserApi;
import cn.bitlinks.ems.module.system.api.user.dto.AdminUserRespDTO;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.nacos.common.utils.CollectionUtils;
import com.alibaba.nacos.common.utils.StringUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.*;
import java.util.function.Function;

import javax.annotation.Resource;
import java.time.LocalDateTime;
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
    @Override
    public Long createEnergyConfiguration(EnergyConfigurationSaveReqVO createReqVO) {
        // 1. 检查能源编码是否重复
        checkEnergyCodeDuplicate(createReqVO.getCode(), null);

        // 2. 插入能源配置主表
        EnergyConfigurationDO energyConfiguration = BeanUtils.toBean(createReqVO, EnergyConfigurationDO.class);
        energyConfigurationMapper.insert(energyConfiguration);
        Long energyId = energyConfiguration.getId();

        // 3. 插入能源参数子表
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

        // 3. 处理单位变更校验（原有逻辑保留）
        String nickname = getLoginUserNickname();
        List<EnergyParametersSaveReqVO> newParameters = updateReqVO.getEnergyParameters();
        String oldUnit = energyConfigurationMapper.selectUnitByEnergyNameAndChinese(String.valueOf(energyId));
        if (CollUtil.isNotEmpty(newParameters) && StringUtils.isNotBlank(oldUnit)) {
            String newUnit = parseUnitFromJson(newParameters);
            if (!newUnit.equals(oldUnit)) {
                List<Long> standingbookIds = standingbookAttributeMapper.selectStandingbookIdByValue(String.valueOf(energyId));
                if (CollectionUtils.isNotEmpty(standingbookIds)) {
                    throw new ServiceException(ENERGY_CONFIGURATION_STANDINGBOOK_UNIT);
                }
            }
        }

        // 4. 更新主表记录
        EnergyConfigurationDO updateObj = BeanUtils.toBean(updateReqVO, EnergyConfigurationDO.class);
        updateObj.setUpdater(nickname);
        energyConfigurationMapper.updateById(updateObj);

        // 5. 处理子表参数（核心逻辑）
        handleEnergyParameters(energyId, newParameters);
    }

    private void handleEnergyParameters(Long energyId, List<EnergyParametersSaveReqVO> newParams) {
        if (newParams == null) {
            return;
        }

        // 获取数据库中现有参数
        List<EnergyParametersDO> oldParams = energyParametersMapper.selectList(
                Wrappers.<EnergyParametersDO>lambdaQuery()
                        .eq(EnergyParametersDO::getEnergyId, energyId)
        );

        // 将参数分为三类：需新增、需更新、需删除
        Map<Long, EnergyParametersSaveReqVO> newParamMap = newParams.stream()
                .filter(p -> p.getId() != null)
                .collect(Collectors.toMap(EnergyParametersSaveReqVO::getId, Function.identity()));

        List<EnergyParametersDO> toDelete = new ArrayList<>();
        List<EnergyParametersSaveReqVO> toUpdate = new ArrayList<>();
        List<EnergyParametersSaveReqVO> toAdd = new ArrayList<>();

        // 遍历旧参数，识别删除或更新
        for (EnergyParametersDO oldParam : oldParams) {
            Long paramId = oldParam.getId();
            if (newParamMap.containsKey(paramId)) {
                // 更新操作
                EnergyParametersSaveReqVO updateVO = newParamMap.get(paramId);
                updateVO.setEnergyId(energyId); // 确保关联正确
                toUpdate.add(updateVO);
            } else {
                // 删除操作
                toDelete.add(oldParam);
            }
        }

        // 识别新增参数（ID为null或不在旧参数ID列表中）
        toAdd = newParams.stream()
                .filter(p -> p.getId() == null || !newParamMap.containsKey(p.getId()))
                .peek(p -> p.setEnergyId(energyId)) // 绑定能源ID
                .collect(Collectors.toList());

        // 执行批量操作
        if (!toDelete.isEmpty()) {
            List<Long> deleteIds = toDelete.stream().map(EnergyParametersDO::getId).collect(Collectors.toList());
            energyParametersMapper.deleteByIds(deleteIds);
        }
        if (!toUpdate.isEmpty()) {
            List<EnergyParametersDO> updateDOs = BeanUtils.toBean(toUpdate, EnergyParametersDO.class);
            energyParametersMapper.updateBatch(updateDOs);
        }
        if (!toAdd.isEmpty()) {
            List<EnergyParametersDO> addDOs = BeanUtils.toBean(toAdd, EnergyParametersDO.class);
            energyParametersMapper.insertBatch(addDOs);
        }

    }

    /**
     * 从 energyParameter JSON 中解析 chinese 为 "用量" 的 unit
     */
    private String parseUnitFromJson(List<EnergyParametersSaveReqVO> parameters) {
        if (CollectionUtils.isEmpty(parameters)) {
            return "";
        }
        try {
            // 使用 Stream API 简化查找逻辑
            return parameters.stream()
                    .filter(param -> "用量".equals(param.getChinese()))
                    .map(EnergyParametersSaveReqVO::getUnit)
                    .filter(StrUtil::isNotBlank)
                    .findFirst()
                    .orElse("");
        } catch (Exception e) {
            throw new ServiceException();
        }
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
            throw new ServiceException(ENERGY_CODE_DUPLICATE);
        }
    }

    private void checkEnergyNameDuplicate(String energyName, Long id) {
        if (StringUtils.isBlank(energyName)) {
            return;
        }
        // 查询是否存在重复名称
        int count = energyConfigurationMapper.countByEnergyNameAndNotId(energyName, id);
        if (count > 0) {
            throw new ServiceException(ENERGY_NAME_DUPLICATE);
        }
    }

    @Override
    public void deleteEnergyConfiguration(Long id) {
        // 校验存在
        validateEnergyConfigurationExists(id);

        // 2. 检查是否存在关联台账
        List<Long> standingbookIds = standingbookAttributeMapper.selectStandingbookIdByValue(String.valueOf(id));
        if (!standingbookIds.isEmpty()) {
            throw exception(ENERGY_CONFIGURATION_STANDINGBOOK_EXISTS);
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

        // 2. 检查是否存在关联台账（任一配置有台账则阻断删除）
        List<Long> invalidIds = ids.stream()
                .filter(id -> {
                    List<Long> standingbookIds = standingbookAttributeMapper.selectStandingbookIdByValue(String.valueOf(id));
                    return CollectionUtils.isNotEmpty(standingbookIds);
                })
                .collect(Collectors.toList());
        if (!invalidIds.isEmpty()) {
            throw exception(ENERGY_CONFIGURATION_STANDINGBOOK_EXISTS, invalidIds.get(0));
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
    public EnergyConfigurationDO getEnergyConfiguration(Long id) {
        // 1. 查询主表
        EnergyConfigurationDO mainDO = energyConfigurationMapper.selectById(id);
        if (mainDO == null) {
            return null;
        }

        // 2. 查询子表（能源参数）
        List<EnergyParametersDO> parameters = energyParametersMapper.selectByEnergyId(id);

        // 3. 组装响应 VO
        EnergyConfigurationDO respVO = BeanUtils.toBean(mainDO, EnergyConfigurationDO.class);
        respVO.setEnergyParameters(parameters);
        return respVO;
    }

    @Override
    public PageResult<EnergyConfigurationDO> getEnergyConfigurationPage(EnergyConfigurationPageReqVO pageReqVO) {
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
        List<EnergyConfigurationDO> voList = pageResult.getList().stream().map(doObj -> {
            // 转换为VO
            EnergyConfigurationDO vo = BeanUtils.toBean(doObj, EnergyConfigurationDO.class);

            // 设置能源参数
            vo.setEnergyParameters(paramsMap.getOrDefault(doObj.getId(), Collections.emptyList()));

            // 原有逻辑：设置单价和创建人昵称
            LocalDateTime currentDateTime = LocalDateTime.now();
            vo.setUnitPrice(unitPriceConfigurationMapper.getPriceDetailsByEnergyIdAndTime(doObj.getId(), currentDateTime));
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
    public List<EnergyConfigurationDO> selectByCondition(String energyName, String energyClassify, String code) {
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
                    EnergyConfigurationDO vo = BeanUtils.toBean(doObj, EnergyConfigurationDO.class);
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
                    .energyParam(updateReqVO.getEnergyParameters())
                    .startEffectiveTime(now)
                    .formulaScale(StrUtil.isNotEmpty(coalScale) ? Integer.valueOf(coalScale) : null)
                    .formulaType(formulaType).build();
        } else {
            // 用能成本公式
            String unitPriceScale = updateReqVO.getUnitPriceScale();
            daParamFormulaDO = DaParamFormulaDO.builder()
                    .energyFormula(updateReqVO.getUnitPriceFormula())
                    .energyId(updateReqVO.getId())
                    .energyParam(updateReqVO.getEnergyParameters())
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
}