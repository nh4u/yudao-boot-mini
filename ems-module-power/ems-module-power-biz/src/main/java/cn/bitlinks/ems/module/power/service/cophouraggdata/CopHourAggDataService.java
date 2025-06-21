package cn.bitlinks.ems.module.power.service.cophouraggdata;

import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StandardCoalInfo;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsParamV2VO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsResultV2VO;

/**
 * @author liumingqiang
 */
public interface CopHourAggDataService {


    StatisticsResultV2VO<StandardCoalInfo> copTable(StatisticsParamV2VO paramVO);
}
