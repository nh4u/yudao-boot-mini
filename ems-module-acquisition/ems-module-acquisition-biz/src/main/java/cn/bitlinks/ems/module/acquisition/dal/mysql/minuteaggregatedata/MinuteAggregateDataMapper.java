package cn.bitlinks.ems.module.acquisition.dal.mysql.minuteaggregatedata;

import cn.bitlinks.ems.framework.tenant.core.aop.TenantIgnore;
import cn.bitlinks.ems.module.acquisition.dal.dataobject.minuteaggregatedata.MinuteAggregateDataDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 实时数据mapper
 */
@Mapper
public interface MinuteAggregateDataMapper {

    /**
     * 获取当前时间的聚合数据
     *
     * @param standingbookId 台账id
     * @param targetTime     聚合时间
     * @return
     */
    @TenantIgnore
    MinuteAggregateDataDO selectExactData(@Param("standingbookId") Long standingbookId,
                                          @Param("targetTime") LocalDateTime targetTime);

    /**
     * 获取指定时间的上一次聚合数据
     * @param standingbookId 台账id
     * @param targetTime 聚合时间
     * @return
     */
    @TenantIgnore
    MinuteAggregateDataDO selectLatestDataByAggTime(@Param("standingbookId") Long standingbookId,
                                          @Param("targetTime") LocalDateTime targetTime);
    /**
     * 获取最新的聚合数据
     *
     * @return 最新的聚合数据
     */
    @TenantIgnore
    List<MinuteAggregateDataDO> getLatestData();


    /**
     * 查询聚合数据中最老的
     *
     * @param standingbookId
     * @return
     */
    @TenantIgnore
    MinuteAggregateDataDO selectOldestByStandingBookId(@Param("standingbookId") Long standingbookId);

    /**
     * 查询聚合数据中最新的数据
     *
     * @param standingbookId
     * @return
     */
    @TenantIgnore
    MinuteAggregateDataDO selectLatestByStandingBookId(@Param("standingbookId") Long standingbookId);

    /**
     * 删除台账的指定时间的数据
     *
     * @param aggregateTime
     * @param standingbookId
     */
    @TenantIgnore
    void deleteDataByMinute(@Param("aggregateTime") LocalDateTime aggregateTime, @Param("standingbookId") Long standingbookId);

    /**
     * 获取该台账的上一个业务点全量值
     * @param standingbookId
     * @return
     */
    @TenantIgnore
    MinuteAggregateDataDO getUsagePrevFullValue(@Param("standingbookId") Long standingbookId,
                                                @Param("targetTime") LocalDateTime targetTime);
    /**
     * 获取该台账的下一个业务点全量值
     * @param standingbookId
     * @return
     */
    @TenantIgnore
    MinuteAggregateDataDO getUsageNextFullValue(@Param("standingbookId") Long standingbookId,
                                                @Param("targetTime") LocalDateTime targetTime);
    /**
     * 获取该台账的当前业务点全量值
     * @param standingbookId
     * @return
     */
    @TenantIgnore
    MinuteAggregateDataDO getUsageExistFullValue(@Param("standingbookId") Long standingbookId,
                                                 @Param("targetTime") LocalDateTime targetTime);
    /**
     * 获取聚合数据
     * @param standingbookIds
     * @param starTime
     * @param endTime
     * @return
     */
    @TenantIgnore
    List<MinuteAggregateDataDO> getRangeDataRequestParam(@Param("standingbookIds")List<Long> standingbookIds, @Param("starTime")LocalDateTime starTime, @Param("endTime")LocalDateTime endTime);


}
