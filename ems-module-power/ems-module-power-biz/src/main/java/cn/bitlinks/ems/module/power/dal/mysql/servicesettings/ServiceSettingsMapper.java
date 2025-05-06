package cn.bitlinks.ems.module.power.dal.mysql.servicesettings;

import java.util.*;

import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.bitlinks.ems.framework.mybatis.core.mapper.BaseMapperX;
import cn.bitlinks.ems.module.power.dal.dataobject.servicesettings.ServiceSettingsDO;
import org.apache.ibatis.annotations.Mapper;
import cn.bitlinks.ems.module.power.controller.admin.servicesettings.vo.*;

/**
 * 服务设置 Mapper
 *
 * @author bitlinks
 */
@Mapper
public interface ServiceSettingsMapper extends BaseMapperX<ServiceSettingsDO> {

    default PageResult<ServiceSettingsDO> selectPage(ServiceSettingsPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ServiceSettingsDO>()
                .likeIfPresent(ServiceSettingsDO::getServiceName, reqVO.getServiceName())
                .eqIfPresent(ServiceSettingsDO::getProtocol, reqVO.getProtocol())
                .eqIfPresent(ServiceSettingsDO::getIpAddress, reqVO.getIpAddress())
                .eqIfPresent(ServiceSettingsDO::getPort, reqVO.getPort())
                .eqIfPresent(ServiceSettingsDO::getRetryCount, reqVO.getRetryCount())
                .eqIfPresent(ServiceSettingsDO::getRegistryId, reqVO.getRegistryId())
                .likeIfPresent(ServiceSettingsDO::getUsername, reqVO.getUsername())
                .eqIfPresent(ServiceSettingsDO::getPassword, reqVO.getPassword())
                .betweenIfPresent(ServiceSettingsDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(ServiceSettingsDO::getId));
    }

}