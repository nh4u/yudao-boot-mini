package cn.bitlinks.ems.module.power.service.report.electricity;

import cn.bitlinks.ems.module.power.controller.admin.report.electricity.vo.ConsumptionStatisticsChartResultVO;
import cn.bitlinks.ems.module.power.controller.admin.report.electricity.vo.ConsumptionStatisticsInfo;
import cn.bitlinks.ems.module.power.controller.admin.report.electricity.vo.ConsumptionStatisticsParamVO;
import cn.bitlinks.ems.module.power.controller.admin.report.electricity.vo.ConsumptionStatisticsResultVO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsParamV2VO;

import java.util.List;

/**
 * 用电量统计 Service 接口
 *
 * @author bmqi
 */
public interface ConsumptionStatisticsService {

    /**
     * 用电量统计
     * @param paramVO
     * @return
     */
    ConsumptionStatisticsResultVO<ConsumptionStatisticsInfo> consumptionStatisticsTable(ConsumptionStatisticsParamVO paramVO);

    ConsumptionStatisticsChartResultVO consumptionStatisticsChart(ConsumptionStatisticsParamVO paramVO);

    List<List<String>> getExcelHeader(ConsumptionStatisticsParamVO paramVO);

    List<List<Object>> getExcelData(ConsumptionStatisticsParamVO paramVO);
}