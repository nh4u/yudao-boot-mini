package cn.bitlinks.ems.module.power.service.usagecost;

import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsParamV2VO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.UsageCostData;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author wangl
 * @date 2025年05月13日 10:51
 */
public interface UsageCostService {
    List<UsageCostData> getList(StatisticsParamV2VO paramVO,
                                LocalDateTime startDate,
                                LocalDateTime endDate,
                                List<Long> standingBookIds);

    LocalDateTime getLastTime(StatisticsParamV2VO paramVO,
                                LocalDateTime startDate,
                                LocalDateTime endDate,
                                List<Long> standingBookIds);


}
