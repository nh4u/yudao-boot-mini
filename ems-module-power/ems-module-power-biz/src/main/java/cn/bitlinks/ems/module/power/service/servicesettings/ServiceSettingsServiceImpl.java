package cn.bitlinks.ems.module.power.service.servicesettings;

import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.framework.common.util.opcda.OpcConnectionTester;
import cn.bitlinks.ems.framework.dict.core.DictFrameworkUtils;
import cn.bitlinks.ems.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.bitlinks.ems.module.power.controller.admin.servicesettings.vo.ServiceSettingsOptionsRespVO;
import cn.bitlinks.ems.module.power.controller.admin.servicesettings.vo.ServiceSettingsPageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.servicesettings.vo.ServiceSettingsSaveReqVO;
import cn.bitlinks.ems.module.power.controller.admin.servicesettings.vo.ServiceSettingsTestReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.servicesettings.ServiceSettingsDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.acquisition.StandingbookAcquisitionDO;
import cn.bitlinks.ems.module.power.dal.mysql.servicesettings.ServiceSettingsMapper;
import cn.bitlinks.ems.module.power.dal.mysql.standingbook.acquisition.StandingbookAcquisitionMapper;
import cn.hutool.core.collection.CollUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.*;
import java.util.function.Predicate;

import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.bitlinks.ems.module.power.enums.CommonConstants.*;
import static cn.bitlinks.ems.module.power.enums.DictTypeConstants.ACQUISITION_PROTOCOL;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.*;

/**
 * 服务设置 Service 实现类
 *
 * @author bitlinks
 */
@Service
@Validated
@Slf4j
public class ServiceSettingsServiceImpl implements ServiceSettingsService {

    @Resource
    private ServiceSettingsMapper serviceSettingsMapper;

    @Resource
    private StandingbookAcquisitionMapper standingbookAcquisitionMapper;
    @Value("${spring.profiles.active}")
    private String env;


    @Override
    public Long createServiceSettings(ServiceSettingsSaveReqVO createReqVO) {
        // 不通也可以添加服务设置
        // 校验 IP 地址是否重复
        validIpAddressRepeat(createReqVO.getIpAddress(), null);
        // 插入
        ServiceSettingsDO serviceSettings = BeanUtils.toBean(createReqVO, ServiceSettingsDO.class);
        serviceSettingsMapper.insert(serviceSettings);
        // 返回
        return serviceSettings.getId();
    }

    /**
     * 校验ip是否重复
     *
     * @param ipAddress ip
     */
    private void validIpAddressRepeat(String ipAddress, Long id) {
        ServiceSettingsDO serviceSettingsDO =
                serviceSettingsMapper.selectOne(new LambdaQueryWrapperX<ServiceSettingsDO>().eq(ServiceSettingsDO::getIpAddress,
                                ipAddress)
                        .neIfPresent(ServiceSettingsDO::getId, id));
        if (serviceSettingsDO != null) {
            throw exception(SERVICE_SETTINGS_IP_REPEAT);
        }

    }

    @Override
    public void updateServiceSettings(ServiceSettingsSaveReqVO updateReqVO) {
        // 校验存在
        ServiceSettingsDO existDO = serviceSettingsMapper.selectById(updateReqVO.getId());
        if (existDO == null) {
            throw exception(SERVICE_SETTINGS_NOT_EXISTS);
        }
        ServiceSettingsDO updateObj = BeanUtils.toBean(updateReqVO, ServiceSettingsDO.class);
        // 设备数采引用后不可编辑（ip、端口、协议、用户名、密码、注册id）
        if (changeProperties(updateObj, existDO)) {
            List<StandingbookAcquisitionDO> existStandingbookAcquisitionDO =
                    standingbookAcquisitionMapper.selectList(StandingbookAcquisitionDO::getServiceSettingsId, updateReqVO.getId());
            if (CollUtil.isNotEmpty(existStandingbookAcquisitionDO)) {
                throw exception(SERVICE_SETTINGS_REFUSE_UPD);
            }
        }

        // 校验 IP 地址是否重复
        validIpAddressRepeat(updateReqVO.getIpAddress(), updateReqVO.getId());
        // 不通也可以添加服务设置
        // 更新
        serviceSettingsMapper.updateById(updateObj);
    }

    /**
     * 比较主要属性是否被修改
     *
     * @param updateObj 修改项
     * @param rawDO     原始
     * @return 是否被修改
     */
    private boolean changeProperties(ServiceSettingsDO updateObj, ServiceSettingsDO rawDO) {
        Predicate<ServiceSettingsDO> isSame = updatedAttr -> updatedAttr.getIpAddress().equals(rawDO.getIpAddress()) &&
                Objects.equals(updatedAttr.getPort(), rawDO.getPort()) &&
                Objects.equals(updatedAttr.getProtocol(), rawDO.getProtocol()) &&
                Objects.equals(updatedAttr.getClsid(), rawDO.getClsid()) &&
                Objects.equals(updatedAttr.getUsername(), rawDO.getUsername()) &&
                Objects.equals(updatedAttr.getPassword(), rawDO.getPassword());

        return !isSame.test(updateObj);
    }

    @Override
    public void deleteServiceSettings(Long id) {
        // 校验存在
        validateServiceSettingsExists(id);
        // 设备数采引用后不可删除
        List<StandingbookAcquisitionDO> existStandingbookAcquisitionDO =
                standingbookAcquisitionMapper.selectList(StandingbookAcquisitionDO::getServiceSettingsId, id);
        if (CollUtil.isNotEmpty(existStandingbookAcquisitionDO)) {
            throw exception(SERVICE_SETTINGS_REFUSE_DELETE);
        }
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

    @Override
    public Boolean testLink(ServiceSettingsTestReqVO createReqVO) {
        if (env.equals(SPRING_PROFILES_ACTIVE_PROD)) {
            return OpcConnectionTester.testLink(createReqVO.getIpAddress(), createReqVO.getUsername(), createReqVO.getPassword(), createReqVO.getClsid(), createReqVO.getRetryCount());
        } else {
            return mockTestLink();
        }
    }


    @Override
    public List<ServiceSettingsOptionsRespVO> getServiceSettingsList() {
        List<ServiceSettingsDO> list = serviceSettingsMapper.selectList();
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        List<ServiceSettingsOptionsRespVO> respVOList = new ArrayList<>();
        list.forEach(serviceSettingsDO -> {
            ServiceSettingsOptionsRespVO respVO = BeanUtils.toBean(serviceSettingsDO, ServiceSettingsOptionsRespVO.class);
            // 服务名称（IP地址：端口号）协议
            respVO.setServiceFormatName(String.format(SERVICE_NAME_FORMAT, serviceSettingsDO.getServiceName(),
                    serviceSettingsDO.getIpAddress(), serviceSettingsDO.getPort(),
                    DictFrameworkUtils.getDictDataLabel(ACQUISITION_PROTOCOL,
                            serviceSettingsDO.getProtocol())));
            respVOList.add(respVO);
        });
        return respVOList;
    }

    /**
     * 随机返回连通结果
     *
     * @return true or false
     */
    private boolean mockTestLink() {
        Random random = new Random();
        double randomNumber = random.nextDouble();

        return randomNumber < SUCCESS_PROBABILITY;
    }

}