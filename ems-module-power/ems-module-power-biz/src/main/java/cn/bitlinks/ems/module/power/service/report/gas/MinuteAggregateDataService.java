package cn.bitlinks.ems.module.power.service.report.gas;

import cn.bitlinks.ems.module.power.dal.dataobject.minuteagg.MinuteAggregateDataDO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 分钟聚合数据service
 */
public interface MinuteAggregateDataService {

    List<MinuteAggregateDataDO> selectByStandingbookIdsAndParamCodes(List<Long> standingbookIds, List<String> paramCodes,
                                                                     LocalDateTime startTime, LocalDateTime endTime);

    List<MinuteAggregateDataDO> selectLastMinuteDataByDate(List<Long> standingbookIds, List<String> paramCodes,
                                                           LocalDateTime startTime, LocalDateTime endTime);

    List<MinuteAggregateDataDO> selectIncrementalSumByDate(List<Long> standingbookIds, List<String> paramCodes,
                                                           LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 批量查询最后一分钟数据 - 性能优化版本
     */
    List<MinuteAggregateDataDO> selectLastMinuteDataByDateBatch(List<Long> standingbookIds, List<String> paramCodes,
                                                                LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 批量查询增量数据之和 - 性能优化版本
     */
    List<MinuteAggregateDataDO> selectIncrementalSumByDateBatch(List<Long> standingbookIds, List<String> paramCodes,
                                                                LocalDateTime startTime, LocalDateTime endTime);
}
