package cn.bitlinks.ems.module.power.service.servicesettings;

import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.framework.common.util.opcda.OpcDaUtils;
import cn.bitlinks.ems.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.bitlinks.ems.module.power.controller.admin.servicesettings.vo.ServiceSettingsPageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.servicesettings.vo.ServiceSettingsSaveReqVO;
import cn.bitlinks.ems.module.power.controller.admin.servicesettings.vo.ServiceSettingsTestReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.servicesettings.ServiceSettingsDO;
import cn.bitlinks.ems.module.power.dal.mysql.servicesettings.ServiceSettingsMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.Objects;
import java.util.Random;
import java.util.function.Predicate;

import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
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

    @Value("${spring.profiles.active}")
    private String env;
    private static final String active = "prod";
    // 成功概率
    private static final double successProbability = 0.8;

    @Override
    public Long createServiceSettings(ServiceSettingsSaveReqVO createReqVO) {
        // 必须连通才可以添加服务设置 todo 待产品确认
        Boolean result = testLink(createReqVO);
        if (!result) {
            throw exception(SERVICE_SETTINGS_ADD_ERROR);
        }
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
        validateServiceSettingsExists(updateReqVO.getId());
        ServiceSettingsDO updateObj = BeanUtils.toBean(updateReqVO, ServiceSettingsDO.class);
        // 设备数采引用后不可编辑（ip、端口、协议、用户名、密码、注册id）todo
//        if(notChangeProperties(updateObj, serviceSettingsMapper.selectById(updateReqVO.getId()))){
//            throw exception(SERVICE_SETTINGS_REFUSE_UPD);
//        }
        // 校验 IP 地址是否重复
        validIpAddressRepeat(updateReqVO.getIpAddress(), updateReqVO.getId());
        // 必须连通才可以修改服务设置 todo 待产品确认
        Boolean result = testLink(updateReqVO);
        if (!result) {
            throw exception(SERVICE_SETTINGS_ADD_ERROR);
        }
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
    private boolean notChangeProperties(ServiceSettingsDO updateObj, ServiceSettingsDO rawDO) {
        Predicate<ServiceSettingsDO> isSame = updatedAttr -> updatedAttr.getIpAddress().equals(rawDO.getIpAddress()) &&
                Objects.equals(updatedAttr.getPort(), rawDO.getPort()) &&
                Objects.equals(updatedAttr.getProtocol(), rawDO.getProtocol()) &&
                Objects.equals(updatedAttr.getClsid(), rawDO.getClsid()) &&
                Objects.equals(updatedAttr.getUsername(), rawDO.getUsername()) &&
                Objects.equals(updatedAttr.getPassword(), rawDO.getPassword());

        return isSame.test(updateObj);
    }

    @Override
    public void deleteServiceSettings(Long id) {
        // 校验存在
        validateServiceSettingsExists(id);
        // 设备数采引用后不可编辑（ip、端口、协议、用户名、密码、注册id）todo
        // throw exception(SERVICE_SETTINGS_REFUSE_DELETE);
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

        String ipAddress = createReqVO.getIpAddress();
        String username = createReqVO.getUsername();
        String password = createReqVO.getPassword();
        String clsid = createReqVO.getClsid();

        Integer retryCount = createReqVO.getRetryCount();  // 获取重试次数

        boolean testResult;
        for (int i = 0; i <= retryCount; i++) { // 注意循环条件，包含第一次尝试

            if (env.equals(active)) {
                testResult = OpcDaUtils.testLink(ipAddress, username, password, clsid);
            } else {
                testResult = mockTestLink();
            }

            if (testResult) {
                return true; // 连接成功，立即返回
            } else {
                if (i < retryCount) {
                    try {
                        Thread.sleep(1000); // 等待一段时间再重试, 这里可以根据实际情况调整
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt(); // 重新设置中断标志
                        return false; // 如果线程被中断，停止重试
                    }
                }
            }
        }

        return false; // 所有重试都失败，返回 false
    }

    /**
     * 随机返回连通结果
     *
     * @return true or false
     */
    private boolean mockTestLink() {
        Random random = new Random();
        double randomNumber = random.nextDouble();

        return randomNumber < successProbability;
    }

}