package cn.bitlinks.ems.module.power.service.warningstrategy;

import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.module.power.controller.admin.warningstrategy.vo.*;

import javax.validation.Valid;
import java.util.List;

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
    WarningStrategyRespVO getWarningStrategy(Long id);

    /**
     * 获得告警策略分页
     *
     * @param pageReqVO 分页查询
     * @return 告警策略分页
     */
    PageResult<WarningStrategyPageRespVO> getWarningStrategyPage(WarningStrategyPageReqVO pageReqVO);

    /**
     * 删除告警策略(批量)
     *
     * @param ids ids
     */
    void deleteWarningStrategyBatch(List<Long> ids);

    /**
     * 批量更新告警策略状态
     *
     * @param updateReqVO updateReqVO
     */
    void updateWarningStrategyStatusBatch(WarningStrategyBatchUpdStatusReqVO updateReqVO);

    /**
     * 批量更新告警策略间隔
     *
     * @param updateReqVO updateReqVO
     */
    void updateWarningStrategyIntervalBatch(WarningStrategyBatchUpdIntervalReqVO updateReqVO);

    /**
     * （内部使用）触发告警【设备编码+设备参数编码+设备参数对应数值】 todo 还差虚拟设备条件处理逻辑，是不是跟计算公式有关系呢？
     */
    void triggerWarning(List<SbDataTriggerVO> sbDataTriggerVOList);
}