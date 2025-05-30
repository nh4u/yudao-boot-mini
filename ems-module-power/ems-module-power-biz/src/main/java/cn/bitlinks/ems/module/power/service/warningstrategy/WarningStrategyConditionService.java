package cn.bitlinks.ems.module.power.service.warningstrategy;

import cn.bitlinks.ems.module.power.controller.admin.warningstrategy.vo.AttributeTreeNode;

import java.util.List;

/**
 * 告警策略条件
 */
public interface WarningStrategyConditionService {
    /**
     * 获取台账属性Tree结构
     * @param standingbookIds 台账ids
     * @param typeIds         台账类型ids
     */
    List<AttributeTreeNode> queryDaqTreeNodeByTypeAndSb(List<Long> standingbookIds, List<Long> typeIds);

}
