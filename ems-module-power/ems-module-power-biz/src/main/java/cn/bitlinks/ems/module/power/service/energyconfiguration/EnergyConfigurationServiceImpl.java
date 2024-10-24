package cn.bitlinks.ems.module.power.service.energyconfiguration;

import cn.bitlinks.ems.module.power.dal.mysql.unitpriceconfiguration.UnitPriceConfigurationMapper;
import com.alibaba.nacos.common.utils.CollectionUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import cn.bitlinks.ems.module.power.controller.admin.energyconfiguration.vo.*;
import cn.bitlinks.ems.module.power.dal.dataobject.energyconfiguration.EnergyConfigurationDO;
import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.common.pojo.PageParam;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;

import cn.bitlinks.ems.module.power.dal.mysql.energyconfiguration.EnergyConfigurationMapper;
import org.thymeleaf.expression.Ids;

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

    @Override
    public Long createEnergyConfiguration(EnergyConfigurationSaveReqVO createReqVO) {
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
        // 更新
        EnergyConfigurationDO updateObj = BeanUtils.toBean(updateReqVO, EnergyConfigurationDO.class);
        energyConfigurationMapper.updateById(updateObj);
    }

    @Override
    public void deleteEnergyConfiguration(Long id) {
        // 校验存在
        validateEnergyConfigurationExists(id);
        // 删除
        energyConfigurationMapper.deleteById(id);
    }

    @Override
    public void deleteEnergyConfigurations(List<Long> ids) {
        // 校验存在
        for (Long id : ids) {
            validateEnergyConfigurationExists(id);
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
        // 遍历结果集，设置 unitPrice
        if (pageResult != null && CollectionUtils.isNotEmpty(pageResult.getList())) {
            for (EnergyConfigurationDO energyConfiguration : pageResult.getList()) {
                String priceDetails = unitPriceConfigurationMapper.getPriceDetailsByEnergyIdAndTime(energyConfiguration.getId(), currentDateTime);
                energyConfiguration.setUnitPrice(priceDetails);
            }
        }
        return pageResult;
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
        Map<Integer, List<EnergyConfigurationDO>> groupedByClassify = energyConfigurations.stream()
                .collect(Collectors.groupingBy(EnergyConfigurationDO::getEnergyClassify));
        return groupedByClassify;
    }

}