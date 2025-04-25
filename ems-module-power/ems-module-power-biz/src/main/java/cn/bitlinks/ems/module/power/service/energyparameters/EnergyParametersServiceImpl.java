package cn.bitlinks.ems.module.power.service.energyparameters;

import com.alibaba.nacos.client.naming.utils.CollectionUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import cn.bitlinks.ems.module.power.controller.admin.energyparameters.vo.*;
import cn.bitlinks.ems.module.power.dal.dataobject.energyparameters.EnergyParametersDO;
import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.common.pojo.PageParam;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;

import cn.bitlinks.ems.module.power.dal.mysql.energyparameters.EnergyParametersMapper;

import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.*;

/**
 * 能源参数 Service 实现类
 *
 * @author bitlinks
 */
@Service
@Validated
public class EnergyParametersServiceImpl implements EnergyParametersService {

    @Resource
    private EnergyParametersMapper energyParametersMapper;

    @Override
    public Long createEnergyParameters(EnergyParametersSaveReqVO createReqVO) {
        // 插入
        EnergyParametersDO energyParameters = BeanUtils.toBean(createReqVO, EnergyParametersDO.class);
        energyParametersMapper.insert(energyParameters);
        // 返回
        return energyParameters.getId();
    }

    @Override
    public void batchCreateEnergyParameters(List<EnergyParametersSaveReqVO> list) {
        if (CollectionUtils.isEmpty(list)) {
            return;
        }
        // 转换为 DO 列表
        List<EnergyParametersDO> doList = BeanUtils.toBean(list, EnergyParametersDO.class);
        // 批量插入（需确保 Mapper 支持）
        energyParametersMapper.insertBatch(doList);
    }

    @Override
    public void updateEnergyParameters(EnergyParametersSaveReqVO updateReqVO) {
        // 校验存在
        validateEnergyParametersExists(updateReqVO.getId());
        // 更新
        EnergyParametersDO updateObj = BeanUtils.toBean(updateReqVO, EnergyParametersDO.class);
        energyParametersMapper.updateById(updateObj);
    }

    @Override
    public void deleteEnergyParameters(Long id) {
        // 校验存在
        validateEnergyParametersExists(id);
        // 删除
        energyParametersMapper.deleteById(id);
    }

    private void validateEnergyParametersExists(Long id) {
        if (energyParametersMapper.selectById(id) == null) {
            throw exception(ENERGY_PARAMETERS_NOT_EXISTS);
        }
    }

    @Override
    public EnergyParametersDO getEnergyParameters(Long id) {
        return energyParametersMapper.selectById(id);
    }

    @Override
    public PageResult<EnergyParametersDO> getEnergyParametersPage(EnergyParametersPageReqVO pageReqVO) {
        return energyParametersMapper.selectPage(pageReqVO);
    }

    @Override
    public List<EnergyParametersDO> getEnergyParametersByEnergyId(Long energyId) {
        LambdaQueryWrapper<EnergyParametersDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(EnergyParametersDO::getEnergyId, energyId);
        return energyParametersMapper.selectList(queryWrapper);
    }

}