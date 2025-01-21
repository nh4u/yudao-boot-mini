package cn.bitlinks.ems.module.power.service.deviceassociationconfiguration;

import java.util.*;
import javax.validation.*;
import cn.bitlinks.ems.module.power.controller.admin.deviceassociationconfiguration.vo.*;
import cn.bitlinks.ems.module.power.dal.dataobject.deviceassociationconfiguration.DeviceAssociationConfigurationDO;
import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.common.pojo.PageParam;

/**
 * 设备关联配置 Service 接口
 *
 * @author bitlinks
 */
public interface DeviceAssociationConfigurationService {

    /**
     * 创建设备关联配置
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createDeviceAssociationConfiguration(@Valid DeviceAssociationConfigurationSaveReqVO createReqVO);

    /**
     * 更新设备关联配置
     *
     * @param updateReqVO 更新信息
     */
    void updateDeviceAssociationConfiguration(@Valid DeviceAssociationConfigurationSaveReqVO updateReqVO);

    /**
     * 删除设备关联配置
     *
     * @param id 编号
     */
    void deleteDeviceAssociationConfiguration(Long id);

    /**
     * 通过计量器具ID获得设备关联配置
     *
     * @param measurementInstrumentId 计量器具ID
     * @return 设备关联配置
     */
    DeviceAssociationConfigurationDO getDeviceAssociationConfigurationByMeasurementInstrumentId(Long measurementInstrumentId);

    /**
     * 获得设备关联配置
     *
     * @param id 编号
     * @return 设备关联配置
     */
    DeviceAssociationConfigurationDO getDeviceAssociationConfiguration(Long id);



    /**
     * 获得设备关联配置分页
     *
     * @param pageReqVO 分页查询
     * @return 设备关联配置分页
     */
    PageResult<DeviceAssociationConfigurationDO> getDeviceAssociationConfigurationPage(DeviceAssociationConfigurationPageReqVO pageReqVO);

    List<DeviceAssociationConfigurationDO> getDeviceAssociationConfigurationList(DeviceAssociationConfigurationPageReqVO pageReqVO);

}