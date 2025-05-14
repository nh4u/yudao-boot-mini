package cn.bitlinks.ems.module.power.service.usagecost;

import java.time.LocalDateTime;
import java.util.List;

import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsParamV2VO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.UsageCostData;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.UsageCostDiscountData;

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

    List<UsageCostDiscountData> getDiscountList(StatisticsParamV2VO paramVO,
                                                LocalDateTime startDate,
                                                LocalDateTime endDate,
                                                List<Long> standingBookIds);


}
