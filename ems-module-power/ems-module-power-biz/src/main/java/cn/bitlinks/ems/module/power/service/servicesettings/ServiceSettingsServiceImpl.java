package cn.bitlinks.ems.module.power.service.servicesettings;

import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import cn.bitlinks.ems.module.power.controller.admin.servicesettings.vo.*;
import cn.bitlinks.ems.module.power.dal.dataobject.servicesettings.ServiceSettingsDO;
import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.common.pojo.PageParam;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;

import cn.bitlinks.ems.module.power.dal.mysql.servicesettings.ServiceSettingsMapper;

import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.*;

/**
 * 服务设置 Service 实现类
 *
 * @author bitlinks
 */
@Service
@Validated
public class ServiceSettingsServiceImpl implements ServiceSettingsService {

    @Resource
    private ServiceSettingsMapper serviceSettingsMapper;

    @Override
    public Long createServiceSettings(ServiceSettingsSaveReqVO createReqVO) {
        // 插入
        ServiceSettingsDO serviceSettings = BeanUtils.toBean(createReqVO, ServiceSettingsDO.class);
        serviceSettingsMapper.insert(serviceSettings);
        // 返回
        return serviceSettings.getId();
    }

    @Override
    public void updateServiceSettings(ServiceSettingsSaveReqVO updateReqVO) {
        // 校验存在
        validateServiceSettingsExists(updateReqVO.getId());
        // 更新
        ServiceSettingsDO updateObj = BeanUtils.toBean(updateReqVO, ServiceSettingsDO.class);
        serviceSettingsMapper.updateById(updateObj);
    }

    @Override
    public void deleteServiceSettings(Long id) {
        // 校验存在
        validateServiceSettingsExists(id);
        // 删除
        serviceSettingsMapper.deleteById(id);
    }

    private void validateServiceSettingsExists(Long id) {
        if (serviceSettingsMapper.selectById(id) == null) {
            throw exception(SERVICE_SETTINGS_NOT_EXISTS);
        }
    }

    @Override
    public ServiceSettingsDO getServiceSettings(Long id) {
        return serviceSettingsMapper.selectById(id);
    }

    @Override
    public PageResult<ServiceSettingsDO> getServiceSettingsPage(ServiceSettingsPageReqVO pageReqVO) {
        return serviceSettingsMapper.selectPage(pageReqVO);
    }

}