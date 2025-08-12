package cn.bitlinks.ems.module.power.service.minuteagg;


import cn.bitlinks.ems.module.power.dal.dataobject.minuteagg.MinuteAggregateDataDO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author liumingqiang
 */
public interface MinuteAggDataService {
    List<MinuteAggregateDataDO> getTmpRangeDataSteady(List<Long> standingbookIds,
                                                      List<String> paramCodes,
                                                      LocalDateTime starTime,

                                                      LocalDateTime endTime);

    /**
     * 根据不同维度获取维度内最大值
     * @param standingbookIds
     * @param paramCodes
     * @param starTime
     * @param endTime
     * @return
     */
    List<MinuteAggregateDataDO> getMaxDataGpByDateType(List<Long> standingbookIds,
                                                      List<String> paramCodes,
                                                      Integer dateType,
                                                      LocalDateTime starTime,
                                                      LocalDateTime endTime);

    LocalDateTime getLastTime(List<Long> standingbookIds,
                              List<String> paramCodes,
                              LocalDateTime starTime,
                              LocalDateTime endTime);
}
