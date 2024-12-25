package cn.bitlinks.ems.module.power.service.statistics;

import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsOverviewResultVO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsParamVO;

/**
 * 统计总览 Service 接口
 *
 * @author hero
 */
public interface StatisticsOverviewService {

    /**
     * 统计总览接口
     *
     * @param paramVO
     * @return
     */
    StatisticsOverviewResultVO overview(StatisticsParamVO paramVO);
}