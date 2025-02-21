package cn.bitlinks.ems.module.power.service.warningstrategy;

import java.util.*;
import javax.validation.*;
import cn.bitlinks.ems.module.power.controller.admin.warningstrategy.vo.*;
import cn.bitlinks.ems.module.power.dal.dataobject.warningstrategy.WarningStrategyDO;
import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.common.pojo.PageParam;

/**
 * 告警策略 Service 接口
 *
 * @author bitlinks
 */
public interface WarningStrategyService {

    /**
     * 创建告警策略
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createWarningStrategy(@Valid WarningStrategySaveReqVO createReqVO);

    /**
     * 更新告警策略
     *
     * @param updateReqVO 更新信息
     */
    void updateWarningStrategy(@Valid WarningStrategySaveReqVO updateReqVO);

    /**
     * 删除告警策略
     *
     * @param id 编号
     */
    void deleteWarningStrategy(Long id);

    /**
     * 获得告警策略
     *
     * @param id 编号
     * @return 告警策略
     */
    WarningStrategyDO getWarningStrategy(Long id);

    /**
     * 获得告警策略分页
     *
     * @param pageReqVO 分页查询
     * @return 告警策略分页
     */
    PageResult<WarningStrategyRespVO> getWarningStrategyPage(WarningStrategyPageReqVO pageReqVO);

    /**
     * 删除告警策略(批量)
     * @param ids ids
     */
    void deleteWarningStrategyBatch(List<Long> ids);

    /**
     * 批量更新告警策略状态
     * @param updateReqVO updateReqVO
     */
    void updateWarningStrategyStatusBatch(WarningStrategyBatchUpdStatusReqVO updateReqVO);

    /**
     * 批量更新告警策略间隔
     * @param updateReqVO updateReqVO
     */
    void updateWarningStrategyIntervalBatch(WarningStrategyBatchUpdIntervalReqVO updateReqVO);
}