package cn.bitlinks.ems.module.power.service.warninginfo;

import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.module.power.controller.admin.warninginfo.vo.WarningInfoPageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.warninginfo.vo.WarningInfoStatisticsRespVO;
import cn.bitlinks.ems.module.power.controller.admin.warninginfo.vo.WarningInfoStatusBatchUpdReqVO;
import cn.bitlinks.ems.module.power.controller.admin.warninginfo.vo.WarningInfoStatusUpdReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.warninginfo.WarningInfoDO;

import java.util.List;

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
     *
     * @return 统计信息
     */
    WarningInfoStatisticsRespVO statistics();

    /**
     * 处理/处理完成 告警消息
     *
     * @param updateReqVO 更新信息
     */
    void updateWarningInfoStatus(WarningInfoStatusUpdReqVO updateReqVO);

    /**
     * 批量处理告警
     *
     * @param reqVO 更新信息
     */
    void updateWarningInfoStatusBatch(WarningInfoStatusBatchUpdReqVO reqVO);

    /**
     * 获得告警信息List
     *
     * @return 告警信息List
     */
    List<WarningInfoDO> getWarningList();

    List<WarningInfoDO> getMonitorListBySbCode(String sbCode);
    WarningInfoStatisticsRespVO getMonitorStatisticsBySbCode(String sbCode);
    long countMonitorBySbCode(String sbCode);
}