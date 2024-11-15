package cn.bitlinks.ems.module.power.service.statistics;

import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsPageReqVO;
import com.alibaba.fastjson.JSONObject;

/**
 * 用能分析 Service 接口
 *
 * @author hero
 */
public interface StatisticsService {

    /**
     *   能留分析图
     * @param pageReqVO 入参
     * @return 数据
     */
    JSONObject energyFlowAnalysis(StatisticsPageReqVO pageReqVO);
}