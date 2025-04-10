package cn.bitlinks.ems.module.power.service.warningstrategy;

import cn.bitlinks.ems.module.power.controller.admin.warningstrategy.vo.SbDataTriggerVO;

import java.util.List;

/**
 * 告警策略触发告警 Service 接口
 *
 * @author bitlinks
 */
public interface WarningStrategyTriggerService {
    /**
     * （内部使用）触发告警【设备编码+设备参数编码+设备参数对应数值】 todo 还差虚拟设备条件处理逻辑，是不是跟计算公式有关系呢？
     */
    void triggerWarning(List<SbDataTriggerVO> sbDataTriggerVOList);
}
