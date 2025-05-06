package cn.bitlinks.ems.module.power.service.servicesettings;

import java.util.*;
import javax.validation.*;
import cn.bitlinks.ems.module.power.controller.admin.servicesettings.vo.*;
import cn.bitlinks.ems.module.power.dal.dataobject.servicesettings.ServiceSettingsDO;
import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.common.pojo.PageParam;

/**
 * 服务设置 Service 接口
 *
 * @author bitlinks
 */
public interface ServiceSettingsService {

    /**
     * 创建服务设置
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createServiceSettings(@Valid ServiceSettingsSaveReqVO createReqVO);

    /**
     * 更新服务设置
     *
     * @param updateReqVO 更新信息
     */
    void updateServiceSettings(@Valid ServiceSettingsSaveReqVO updateReqVO);

    /**
     * 删除服务设置
     *
     * @param id 编号
     */
    void deleteServiceSettings(Long id);

    /**
     * 获得服务设置
     *
     * @param id 编号
     * @return 服务设置
     */
    ServiceSettingsDO getServiceSettings(Long id);

    /**
     * 获得服务设置分页
     *
     * @param pageReqVO 分页查询
     * @return 服务设置分页
     */
    PageResult<ServiceSettingsDO> getServiceSettingsPage(ServiceSettingsPageReqVO pageReqVO);

    /**
     * 测试服务连通
     * @param createReqVO 服务数据
     * @return 是否连通
     */
    Boolean testLink(ServiceSettingsTestReqVO createReqVO);
}