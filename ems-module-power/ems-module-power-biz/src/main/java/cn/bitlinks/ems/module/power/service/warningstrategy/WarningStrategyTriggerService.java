package cn.bitlinks.ems.module.power.service.warningstrategy;

import java.time.LocalDateTime;

/**
 * 告警策略触发告警 Service 接口
 *
 * @author bitlinks
 */
public interface WarningStrategyTriggerService {
    /**
     * （内部使用）查询该策略对应的所有设备参数的最新实时数据,进行告警.
     */
    void triggerWarning(Long strategyId, LocalDateTime triggerTime);
}
