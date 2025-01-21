package cn.bitlinks.ems.module.power.service.deviceassociationconfiguration;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import cn.bitlinks.ems.module.power.controller.admin.deviceassociationconfiguration.vo.*;
import cn.bitlinks.ems.module.power.dal.dataobject.deviceassociationconfiguration.DeviceAssociationConfigurationDO;
import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.common.pojo.PageParam;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;

import cn.bitlinks.ems.module.power.dal.mysql.deviceassociationconfiguration.DeviceAssociationConfigurationMapper;

import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.*;

/**
 * 设备关联配置 Service 实现类
 *
 * @author bitlinks
 */
@Service
@Validated
public class DeviceAssociationConfigurationServiceImpl implements DeviceAssociationConfigurationService {

    @Resource
    private DeviceAssociationConfigurationMapper deviceAssociationConfigurationMapper;

    @Override
    public Long createDeviceAssociationConfiguration(DeviceAssociationConfigurationSaveReqVO createReqVO) {
        // 插入
        DeviceAssociationConfigurationDO deviceAssociationConfiguration = BeanUtils.toBean(createReqVO, DeviceAssociationConfigurationDO.class);
        deviceAssociationConfigurationMapper.insert(deviceAssociationConfiguration);
        // 返回
        return deviceAssociationConfiguration.getId();
    }

    @Override
    public void updateDeviceAssociationConfiguration(DeviceAssociationConfigurationSaveReqVO updateReqVO) {
        // 校验存在
        validateDeviceAssociationConfigurationExists(updateReqVO.getId());
        // 更新
        DeviceAssociationConfigurationDO updateObj = BeanUtils.toBean(updateReqVO, DeviceAssociationConfigurationDO.class);
        deviceAssociationConfigurationMapper.updateById(updateObj);
    }

    @Override
    public void deleteDeviceAssociationConfiguration(Long id) {
        // 校验存在
        validateDeviceAssociationConfigurationExists(id);
        // 删除
        deviceAssociationConfigurationMapper.deleteById(id);
    }

    private void validateDeviceAssociationConfigurationExists(Long id) {
        if (deviceAssociationConfigurationMapper.selectById(id) == null) {
            throw exception(DEVICE_ASSOCIATION_CONFIGURATION_NOT_EXISTS);
        }
    }

    @Override
    public DeviceAssociationConfigurationDO getDeviceAssociationConfiguration(Long id) {
        return deviceAssociationConfigurationMapper.selectById(id);
    }

    @Override
    public PageResult<DeviceAssociationConfigurationDO> getDeviceAssociationConfigurationPage(DeviceAssociationConfigurationPageReqVO pageReqVO) {
        return deviceAssociationConfigurationMapper.selectPage(pageReqVO);
    }

    @Override
    public DeviceAssociationConfigurationDO getDeviceAssociationConfigurationByMeasurementInstrumentId(Long measurementInstrumentId) {
        QueryWrapper<DeviceAssociationConfigurationDO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("measurement_instrument_id", measurementInstrumentId);
        return deviceAssociationConfigurationMapper.selectOne(queryWrapper);
    }

    @Override
    public List<DeviceAssociationConfigurationDO> getDeviceAssociationConfigurationList(DeviceAssociationConfigurationPageReqVO pageReqVO) {
        return deviceAssociationConfigurationMapper.selectList(pageReqVO);
    }

}