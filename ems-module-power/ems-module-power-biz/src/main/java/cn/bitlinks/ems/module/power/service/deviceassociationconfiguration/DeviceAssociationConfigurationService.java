package cn.bitlinks.ems.module.power.service.deviceassociationconfiguration;

import cn.bitlinks.ems.module.power.controller.admin.deviceassociationconfiguration.vo.DeviceAssociationSaveReqVO;
import cn.bitlinks.ems.module.power.controller.admin.deviceassociationconfiguration.vo.MeasurementAssociationSaveReqVO;

/**
 * 关联计量器具+设备 Service 接口
 *
 * @author bitlinks
 */
public interface DeviceAssociationConfigurationService {


    /**
     * 关联下级计量器具
     *
     * @param createReqVO 下级计量器具ids
     */
    void updAssociationMeasurementInstrument(MeasurementAssociationSaveReqVO createReqVO);

    /**
     * 关联上级设备
     *
     * @param createReqVO 上级设备id
     */
    void updAssociationDevice(DeviceAssociationSaveReqVO createReqVO);
}