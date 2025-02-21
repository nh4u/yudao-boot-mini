package cn.bitlinks.ems.module.power.dal.mysql.deviceassociationconfiguration;

import java.util.*;

import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.bitlinks.ems.framework.mybatis.core.mapper.BaseMapperX;
import cn.bitlinks.ems.module.power.dal.dataobject.deviceassociationconfiguration.DeviceAssociationConfigurationDO;
import org.apache.ibatis.annotations.Mapper;
import cn.bitlinks.ems.module.power.controller.admin.deviceassociationconfiguration.vo.*;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 设备关联配置 Mapper
 *
 * @author bitlinks
 */
@Mapper
public interface DeviceAssociationConfigurationMapper extends BaseMapperX<DeviceAssociationConfigurationDO> {

    default PageResult<DeviceAssociationConfigurationDO> selectPage(DeviceAssociationConfigurationPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<DeviceAssociationConfigurationDO>()
                .eqIfPresent(DeviceAssociationConfigurationDO::getEnergyId, reqVO.getEnergyId())
                .eqIfPresent(DeviceAssociationConfigurationDO::getMeasurementInstrumentId, reqVO.getMeasurementInstrumentId())
                .eqIfPresent(DeviceAssociationConfigurationDO::getMeasurementIds, reqVO.getMeasurementIds())
                .eqIfPresent(DeviceAssociationConfigurationDO::getDeviceId, reqVO.getDeviceId())
                .betweenIfPresent(DeviceAssociationConfigurationDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(DeviceAssociationConfigurationDO::getId));
    }

    default List<DeviceAssociationConfigurationDO> selectList(DeviceAssociationConfigurationPageReqVO reqVO) {
        return selectList(new LambdaQueryWrapperX<DeviceAssociationConfigurationDO>()
                .eqIfPresent(DeviceAssociationConfigurationDO::getEnergyId, reqVO.getEnergyId())
                .eqIfPresent(DeviceAssociationConfigurationDO::getMeasurementInstrumentId, reqVO.getMeasurementInstrumentId())
                .eqIfPresent(DeviceAssociationConfigurationDO::getMeasurementIds, reqVO.getMeasurementIds())
                .eqIfPresent(DeviceAssociationConfigurationDO::getDeviceId, reqVO.getDeviceId())
                .betweenIfPresent(DeviceAssociationConfigurationDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(DeviceAssociationConfigurationDO::getId));
    }

    @Select("SELECT * FROM ems_device_association_configuration WHERE energy_id = #{energyId} AND measurement_instrument_id = #{instrumentId}")
    DeviceAssociationConfigurationSaveReqVO selectByEnergyAndInstrument(
            @Param("energyId") Long energyId,
            @Param("instrumentId") Long measurementInstrumentId
    );
}