package cn.bitlinks.ems.module.acquisition.service.minuteaggregatedata;

import cn.bitlinks.ems.module.acquisition.api.collectrawdata.dto.MinuteAggDataSplitDTO;
import cn.bitlinks.ems.module.acquisition.api.collectrawdata.dto.MinuteAggregateDataDTO;
import cn.bitlinks.ems.module.acquisition.dal.dataobject.minuteaggregatedata.MinuteAggregateDataDO;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 分钟聚合数据service
 */
public interface MinuteAggregateDataService {
    /**
     * 获取指定时间的聚合数据
     *
     * @param standingbookId  台账id
     * @param thisCollectTime 指定聚合时间
     * @return 聚合数据
     */
    MinuteAggregateDataDTO selectByAggTime(Long standingbookId, LocalDateTime thisCollectTime);

    /**
     * 获取指定时间的上次聚合数据
     *
     * @param standingbookId     台账id
     * @param currentCollectTime 指定聚合时间
     * @return 聚合数据
     */
    MinuteAggregateDataDTO selectLatestByAggTime(Long standingbookId, LocalDateTime currentCollectTime);

    /**
     * 查询台账最老数据
     *
     * @param standingbookId
     * @return
     */
    MinuteAggregateDataDTO selectOldestByStandingBookId(Long standingbookId);

    /**
     * 查询台账最新数据
     *
     * @param standingbookId
     * @return
     */
    MinuteAggregateDataDTO selectLatestByStandingBookId(Long standingbookId);

    /**
     * 通用的聚合数据改动发送给usagecost
     *
     * @param aggDataList
     * @throws IOException
     */
    void sendMsgToUsageCostBatch(List<MinuteAggregateDataDO> aggDataList) throws IOException;

    /**
     * 插入时间段数据，需要拆分，起始数据存在
     *
     * @param minuteAggDataSplitDTO
     */
    void insertRangeData(MinuteAggDataSplitDTO minuteAggDataSplitDTO);
}
