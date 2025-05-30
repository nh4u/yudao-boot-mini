package cn.bitlinks.ems.module.power.service.warninginfo;

import javax.validation.*;
import cn.bitlinks.ems.module.power.controller.admin.warninginfo.vo.*;
import cn.bitlinks.ems.module.power.dal.dataobject.warninginfo.WarningInfoDO;
import cn.bitlinks.ems.framework.common.pojo.PageResult;

/**
 * 告警信息 Service 接口
 *
 * @author bitlinks
 */
public interface WarningInfoService {


    /**
     * 获得告警信息
     *
     * @param id 编号
     * @return 告警信息
     */
    WarningInfoDO getWarningInfo(Long id);

    /**
     * 获得告警信息分页
     *
     * @param pageReqVO 分页查询
     * @return 告警信息分页
     */
    PageResult<WarningInfoDO> getWarningInfoPage(WarningInfoPageReqVO pageReqVO);

    /**
     * 告警信息统计
     * @return 统计信息
     */
    WarningInfoStatisticsRespVO statistics();

    /**
     * 处理/处理完成 告警消息
     * @param updateReqVO 更新信息
     */
    void updateWarningInfoStatus(WarningInfoStatusUpdReqVO updateReqVO);
}