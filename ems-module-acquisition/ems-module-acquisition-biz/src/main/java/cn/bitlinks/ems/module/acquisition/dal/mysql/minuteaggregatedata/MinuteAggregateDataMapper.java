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
     * 获取最新的聚合数据
     *
     * @return 最新的聚合数据
     */
    @TenantIgnore
    List<MinuteAggregateDataDO> getLatestData();


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
    List<MinuteAggregateDataDO> getCopRangeData(@Param("standingbookIds")List<Long> standingbookIds, @Param("paramCodes")List<String> paramCodes, @Param("starTime")LocalDateTime starTime, @Param("endTime")LocalDateTime endTime);
    /**
     * 获取聚合数据
     * @param standingbookIds
     * @param starTime
     * @param endTime
     * @return
     */
    @TenantIgnore
    List<MinuteAggregateDataDO> getCopRangeDataSteady(@Param("standingbookIds")List<Long> standingbookIds, @Param("paramCodes")List<String> paramCodes, @Param("starTime")LocalDateTime starTime, @Param("endTime")LocalDateTime endTime);
    /**
     * 获取多个台账的上一个业务点全量值
     * @return
     */
    @TenantIgnore
    List<MinuteAggregateDataDO> getSbIdsUsagePrevFullValue(@Param("standingbookIds")List<Long> standingbookIds, @Param("targetTime")LocalDateTime targetTime);
    /**
     * 获取多个台账的下一个业务点全量值
     * @return
     */
    @TenantIgnore
    List<MinuteAggregateDataDO> getSbIdsUsageNextFullValue(@Param("standingbookIds")List<Long> standingbookIds, @Param("targetTime")LocalDateTime targetTime);

}
