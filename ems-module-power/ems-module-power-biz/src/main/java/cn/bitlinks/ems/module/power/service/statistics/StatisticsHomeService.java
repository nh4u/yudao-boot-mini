package cn.bitlinks.ems.module.power.service.statistics;

import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.*;

import javax.validation.Valid;
import java.math.BigDecimal;

/**
 * 统计总览 Service 接口
 */
public interface StatisticsHomeService {

    /**
     * 统计总览接口
     *
     * @param paramVO
     * @return
     */
    StatisticsHomeResultVO overview(StatisticsParamHomeVO paramVO);

    StatisticsHomeBarVO<BigDecimal> costChart(StatisticsParamHomeVO paramVO);

    StatisticsHomeBarVO<BigDecimal> coalChart(StatisticsParamHomeVO paramVO);

    /**
     * 统计总览-顶部设备数量
     *
     * @return vo
     */
    StatisticsHomeTopResultVO overviewTop();

    /**
     * 统计总览-顶部 单位能源、查询条件时间维度
     *
     * @param paramVO timeRange
     * @return vo
     */
    StatisticsHomeTop2ResultVO overviewTop2(@Valid StatisticsParamHomeVO paramVO);
}
