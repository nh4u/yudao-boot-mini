package cn.bitlinks.ems.module.power.service.energyconfiguration;

import cn.bitlinks.ems.framework.common.exception.ServiceException;
import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.module.power.controller.admin.energyconfiguration.vo.EnergyConfigurationPageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.energyconfiguration.vo.EnergyConfigurationSaveReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.daparamformula.DaParamFormulaDO;
import cn.bitlinks.ems.module.power.dal.dataobject.energyconfiguration.EnergyConfigurationDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.type.StandingbookTypeDO;
import cn.bitlinks.ems.module.power.dal.mysql.daparamformula.DaParamFormulaMapper;
import cn.bitlinks.ems.module.power.dal.mysql.energyconfiguration.EnergyConfigurationMapper;
import cn.bitlinks.ems.module.power.dal.mysql.standingbook.attribute.StandingbookAttributeMapper;
import cn.bitlinks.ems.module.power.dal.mysql.unitpriceconfiguration.UnitPriceConfigurationMapper;
import cn.bitlinks.ems.module.power.service.daparamformula.DaParamFormulaService;
import cn.bitlinks.ems.module.power.service.standingbook.StandingbookService;
import cn.bitlinks.ems.module.system.api.user.AdminUserApi;
import cn.bitlinks.ems.module.system.api.user.dto.AdminUserRespDTO;
import cn.hutool.core.util.StrUtil;
import com.alibaba.nacos.common.utils.CollectionUtils;
import com.alibaba.nacos.common.utils.StringUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
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
    private StandingbookAttributeMapper standingbookAttributeMapper ;

    @Resource
    private DaParamFormulaMapper daParamFormulaMapper;
    @Resource
    private AdminUserApi adminUserApi;

    @Override
    public Long createEnergyConfiguration(EnergyConfigurationSaveReqVO createReqVO) {
        // 检查能源编码是否重复
        checkEnergyCodeDuplicate(createReqVO.getCode(), null);
        // 插入
        EnergyConfigurationDO energyConfiguration = BeanUtils.toBean(createReqVO, EnergyConfigurationDO.class);
        energyConfigurationMapper.insert(energyConfiguration);
        // 返回
        return energyConfiguration.getId();
    }

    @Override
    public void updateEnergyConfiguration(EnergyConfigurationSaveReqVO updateReqVO) {
        // 校验存在
        validateEnergyConfigurationExists(updateReqVO.getId());
        // 检查能源编码是否重复（排除自身）
        checkEnergyCodeDuplicate(updateReqVO.getCode(), updateReqVO.getId());
        String energyName = updateReqVO.getEnergyName();
        String unit = energyConfigurationMapper.selectUnitByEnergyNameAndChinese(energyName);
        List<Long> standingbookIds = standingbookAttributeMapper.selectStandingbookIdByValue(energyName);
        if (standingbookIds != null && unit!= null) {
            throw new ServiceException(ENERGY_CONFIGURATION_STANDINGBOOK_UNIT);
        }
        // 更新
        EnergyConfigurationDO updateObj = BeanUtils.toBean(updateReqVO, EnergyConfigurationDO.class);
        energyConfigurationMapper.updateById(updateObj);
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
        String energyName = energyConfigurationMapper.selectById(id).getEnergyName();

        // 使用 selectList 查询所有关联的台账 ID
        List<Long> standingbookIds = standingbookAttributeMapper.selectStandingbookIdByValue(energyName);
        if (!standingbookIds.isEmpty()) {
            throw exception(ENERGY_CONFIGURATION_STANDINGBOOK_EXISTS);
        }
        // 删除
        energyConfigurationMapper.deleteById(id);
    }

    @Override
    public void deleteEnergyConfigurations(List<Long> ids) {
        // 校验存在
        for (Long id : ids) {
            validateEnergyConfigurationExists(id);
            String energyName = energyConfigurationMapper.selectById(id).getEnergyName();
            // 使用 selectList 查询所有关联的台账 ID
            List<Long> standingbookIds = standingbookAttributeMapper.selectStandingbookIdByValue(energyName);
            if (!standingbookIds.isEmpty()) {
                throw exception(ENERGY_CONFIGURATION_STANDINGBOOK_EXISTS);
            }
        }
        // 删除
        energyConfigurationMapper.deleteByIds(ids);
    }

    private void validateEnergyConfigurationExists(Long id) {
        if (energyConfigurationMapper.selectById(id) == null) {
            throw exception(ENERGY_CONFIGURATION_NOT_EXISTS);
        }
    }

    @Override
    public EnergyConfigurationDO getEnergyConfiguration(Long id) {
        return energyConfigurationMapper.selectById(id);
    }

    @Override
    public PageResult<EnergyConfigurationDO> getEnergyConfigurationPage(EnergyConfigurationPageReqVO pageReqVO) {
        // 获取当前时间
        LocalDateTime currentDateTime = LocalDateTime.now();
        // 查询分页数据
        PageResult<EnergyConfigurationDO> pageResult = energyConfigurationMapper.selectPage(pageReqVO);
        // 遍历结果集，设置 unitPrice 和 creator 昵称
        if (pageResult != null && CollectionUtils.isNotEmpty(pageResult.getList())) {
            for (EnergyConfigurationDO energyConfiguration : pageResult.getList()) {
                // 设置单价详情
                String priceDetails = unitPriceConfigurationMapper.getPriceDetailsByEnergyIdAndTime(energyConfiguration.getId(), currentDateTime);
                Integer billingMethod = unitPriceConfigurationMapper.getBillingMethodByEnergyIdAndTime(energyConfiguration.getId(), currentDateTime);
                Integer accountingFrequency = unitPriceConfigurationMapper.getAccountingFrequencyByEnergyIdAndTime(energyConfiguration.getId(), currentDateTime);;
                energyConfiguration.setUnitPrice(priceDetails);
                energyConfiguration.setBillingMethod(billingMethod);
                energyConfiguration.setAccountingFrequency(accountingFrequency);

                // 设置创建人昵称
                if (energyConfiguration.getCreator() != null) {
                    CommonResult<AdminUserRespDTO> user = adminUserApi.getUser(Long.valueOf(energyConfiguration.getCreator()));
                    if (user.getData() != null) {
                        energyConfiguration.setCreator(user.getData().getNickname());
                    }
                }
            }
        }
        return pageResult;
    }

    @Override
    public List<EnergyConfigurationDO> getEnergyConfigurationList(EnergyConfigurationPageReqVO queryVO) {
        return energyConfigurationMapper.selectList(queryVO);
    }

    @Override
    public List<EnergyConfigurationDO> selectByCondition(String energyName, String energyClassify, String code) {
        QueryWrapper<EnergyConfigurationDO> queryWrapper = new QueryWrapper<>();

        if (energyName != null && !energyName.isEmpty()) {
            queryWrapper.eq("energy_name", energyName);
        }
        if (energyClassify != null && !energyClassify.isEmpty()) {
            queryWrapper.eq("energy_classify", energyClassify);
        }
        if (code != null && !code.isEmpty()) {
            queryWrapper.eq("code", code);
        }

        return energyConfigurationMapper.selectList(queryWrapper);
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
                    .energyParam(updateReqVO.getEnergyParameter())
                    .startEffectiveTime(now)
                    .formulaScale(StrUtil.isNotEmpty(coalScale) ? Integer.valueOf(coalScale) : null)
                    .formulaType(formulaType).build();
        } else {
            // 折标煤公式
            String unitPriceScale = updateReqVO.getUnitPriceScale();
            daParamFormulaDO = DaParamFormulaDO.builder()
                    .energyFormula(updateReqVO.getUnitPriceFormula())
                    .energyId(updateReqVO.getId())
                    .energyParam(updateReqVO.getEnergyParameter())
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