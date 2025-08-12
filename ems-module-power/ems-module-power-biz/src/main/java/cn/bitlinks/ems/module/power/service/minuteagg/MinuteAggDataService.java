package cn.bitlinks.ems.module.power.service.minuteagg;


import cn.bitlinks.ems.module.power.dal.dataobject.minuteagg.SupplyWaterTmpMinuteAggData;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author liumingqiang
 */
public interface MinuteAggDataService {
    List<SupplyWaterTmpMinuteAggData> getTmpRangeDataSteady(List<Long> standingbookIds,
                                                            List<String> paramCodes,
                                                            LocalDateTime starTime,
                                                            LocalDateTime endTime);

    LocalDateTime getLastTime(List<Long> standingbookIds,
                              List<String> paramCodes,
                              LocalDateTime starTime,
                              LocalDateTime endTime);
}
