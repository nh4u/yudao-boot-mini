package cn.bitlinks.ems.module.power.service.minuteagg;


import cn.bitlinks.ems.module.power.controller.admin.report.electricity.vo.MinuteAggDataDTO;
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

    /**
     * 根据不同维度获取维度内最大值
     * @param standingbookIds
     * @param paramCodes
     * @param starTime
     * @param endTime
     * @return
     */
    List<MinuteAggDataDTO> getMaxDataGpByDateType(List<Long> standingbookIds,
                                                  List<String> paramCodes,
                                                  Integer dateType,
                                                  LocalDateTime starTime,
                                                  LocalDateTime endTime);

    LocalDateTime getLastTime(List<Long> standingbookIds,
                              List<String> paramCodes,
                              LocalDateTime starTime,
                              LocalDateTime endTime);
}
