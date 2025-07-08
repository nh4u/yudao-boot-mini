package cn.bitlinks.ems.module.acquisition.service.minuteaggregatedata;

import cn.bitlinks.ems.module.acquisition.api.collectrawdata.dto.MinuteAggDataSplitDTO;
import cn.bitlinks.ems.module.acquisition.api.collectrawdata.dto.MinuteAggregateDataDTO;
import cn.bitlinks.ems.module.acquisition.api.minuteaggregatedata.dto.MinuteRangeDataParamDTO;
import cn.bitlinks.ems.module.acquisition.dal.dataobject.minuteaggregatedata.MinuteAggregateDataDO;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 分钟聚合数据service
 */
public interface MinuteAggregateDataService {

    /**
     * 聚合数据的新增[稳态值]
     *
     * @param aggDataList
     * @throws IOException
     */
    void insertSteadyAggDataBatch(List<MinuteAggregateDataDO> aggDataList) throws IOException;

    /**
     * 通用的聚合数据改动发送给usagecost
     *
     * @param aggDataList
     * @throws IOException
     */
    void sendMsgToUsageCostBatch(List<MinuteAggregateDataDO> aggDataList, Boolean copFlag) throws IOException;

    /**
     * 插入单条数据，初始化数据
     */
    void insertDataBatch(List<MinuteAggregateDataDTO> minuteAggregateDataDTOList);

    /**
     * 插入时间段数据，需要拆分，起始数据存在
     *
     * @param minuteAggDataSplitDTO
     */
    void insertRangeData(MinuteAggDataSplitDTO minuteAggDataSplitDTO);

    /**
     * 获取指定时间段的聚合数据
     *
     * @param standingbookIds
     * @param starTime
     * @param endTime
     * @return
     */
    List<MinuteAggregateDataDTO> getCopRangeData(List<Long> standingbookIds,List<String> paramCodes, LocalDateTime starTime, LocalDateTime endTime);
    /**
     * 获取指定时间段的聚合数据
     *
     * @param standingbookIds
     * @param starTime
     * @param endTime
     * @return
     */
    List<MinuteAggregateDataDTO> getCopRangeDataSteady(List<Long> standingbookIds,List<String> paramCodes, LocalDateTime starTime, LocalDateTime endTime);
    /**
     * 获取该台账的上一个全量值
     * @param standingbookId
     * @param acquisitionTime
     * @return
     */
    MinuteAggregateDataDTO getUsagePrevFullValue(Long standingbookId, LocalDateTime acquisitionTime);

    /**
     * 获取该台账的下一个全量值
     * @param standingbookId
     * @param acquisitionTime
     * @return
     */
    MinuteAggregateDataDTO getUsageNextFullValue(Long standingbookId, LocalDateTime acquisitionTime);

    /**
     * 获取该台账的当前业务点全量值
     * @param standingbookId
     * @param acquisitionTime
     * @return
     */
    MinuteAggregateDataDTO getUsageExistFullValue(Long standingbookId, LocalDateTime acquisitionTime);

    /**
     * 获取时间段首尾两端附近的数据
     * @return
     */
    Map<Long, MinuteAggDataSplitDTO> getPreAndNextData(MinuteRangeDataParamDTO minuteRangeDataParamDTO);
}
