package cn.bitlinks.ems.module.power.service.statistics.deviation;

import cn.bitlinks.ems.module.power.controller.admin.statistics.deviation.vo.DeviationChartResultVO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.deviation.vo.DeviationChartYInfo;
import cn.bitlinks.ems.module.power.controller.admin.statistics.deviation.vo.DeviationStatisticsParamVO;

/**
 * 用能分析 同比分析 Service 接口
 *
 * @author hero
 */
public interface DeviationService {


    DeviationChartResultVO<DeviationChartYInfo> deviationChart(DeviationStatisticsParamVO paramVO);
}
