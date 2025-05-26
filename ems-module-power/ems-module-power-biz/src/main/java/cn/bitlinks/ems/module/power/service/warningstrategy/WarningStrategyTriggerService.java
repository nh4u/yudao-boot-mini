package cn.bitlinks.ems.module.power.service.warningstrategy;

import cn.bitlinks.ems.module.power.dal.dataobject.warningstrategy.WarningStrategyDO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 告警策略触发告警 Service 接口
 *
 * @author bitlinks
 */
public interface WarningStrategyTriggerService {
    /**
     * （内部使用）查询该策略对应的所有设备参数的最新实时数据,进行告警.
     */
    void triggerWarning(List<WarningStrategyDO> warningStrategyDOS, LocalDateTime triggerTime);
}
