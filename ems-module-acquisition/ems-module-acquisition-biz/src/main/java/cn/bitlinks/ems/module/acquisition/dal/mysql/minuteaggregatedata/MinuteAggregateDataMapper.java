package cn.bitlinks.ems.module.acquisition.dal.mysql.minuteaggregatedata;

import cn.bitlinks.ems.framework.tenant.core.aop.TenantIgnore;
import cn.bitlinks.ems.module.acquisition.api.collectrawdata.dto.MinuteAggregateDataDTO;
import cn.bitlinks.ems.module.acquisition.dal.dataobject.minuteaggregatedata.MinuteAggregateDataDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 实时数据mapper
 */
@Mapper
public interface MinuteAggregateDataMapper {

//    /**
//     * 获取最新的聚合数据
//     * @return 最新的聚合数据
//     */
//    List<MinuteAggregateDataDO> getLatestData();

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
     * 批量插入
     *
     * @param aggregateDataDOS
     */
    @Insert("<script>" +
            "INSERT INTO minute_aggregate_data (aggregate_time, param_code, energy_flag, data_site, standingbook_id, " +
            "full_value, incremental_value) VALUES " +
            "<foreach collection='aggregateDataDOS' item='aggregateDataDO' separator=','>" +
            "(#{aggregateDataDO.aggregateTime}, #{aggregateDataDO.paramCode}, #{aggregateDataDO.energyFlag}, " +
            "#{aggregateDataDO.dataSite}, " +
            "#{aggregateDataDO.standingbookId}, #{aggregateDataDO.fullValue},#{aggregateDataDO.incrementalValue})" +
            "</foreach>" +
            "</script>")
    @TenantIgnore
    void insertBatch(@Param("aggregateDataDOS") List<MinuteAggregateDataDO> aggregateDataDOS);

    /**
     * 查询聚合数据中最老的
     * @param standingbookId
     * @return
     */
    MinuteAggregateDataDO selectOldestByStandingBookId(@Param("standingbookId")Long standingbookId);

    /**
     * 查询聚合数据中最新的数据
     * @param standingbookId
     * @return
     */

    MinuteAggregateDataDO selectLatestByStandingBookId(@Param("standingbookId")Long standingbookId);

    /**
     * 删除台账的指定时间的数据
     * @param aggregateTime
     * @param standingbookId
     */
    void deleteDataByMinute(@Param("aggregateTime")LocalDateTime aggregateTime, @Param("standingbookId") Long standingbookId);
}
