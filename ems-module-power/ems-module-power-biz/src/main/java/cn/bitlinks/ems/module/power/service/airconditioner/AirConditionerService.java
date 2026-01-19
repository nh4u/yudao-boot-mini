package cn.bitlinks.ems.module.power.service.airconditioner;


import cn.bitlinks.ems.module.power.controller.admin.airconditioner.vo.AirConditionerSettingsReqVO;
import cn.bitlinks.ems.module.power.controller.admin.report.electricity.vo.ConsumptionStatisticsChartResultVO;
import cn.bitlinks.ems.module.power.controller.admin.report.electricity.vo.ConsumptionStatisticsChartYInfo;

import javax.validation.Valid;
import java.util.List;

public interface AirConditionerService {
    /**
     * 获取选项列表
     *
     * @return 返回一个包含字符串选项的List集合
     */
    List<String> getOptions();

    /**
     * 图
     * @param paramVO
     * @return
     */
    ConsumptionStatisticsChartResultVO<ConsumptionStatisticsChartYInfo> getChart(@Valid AirConditionerSettingsReqVO paramVO);
}
