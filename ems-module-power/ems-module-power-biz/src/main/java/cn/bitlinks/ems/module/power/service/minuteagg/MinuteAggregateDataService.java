package cn.bitlinks.ems.module.power.service.minuteagg;


import cn.bitlinks.ems.module.power.dal.dataobject.minuteagg.MinuteAggregateDataDO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author liumingqiang
 */
public interface MinuteAggregateDataService {
    List<MinuteAggregateDataDO> getTmpRangeDataSteady(List<Long> standingbookIds,
                                                      List<String> paramCodes,
                                                      LocalDateTime starTime,
                                                      LocalDateTime endTime);

}
