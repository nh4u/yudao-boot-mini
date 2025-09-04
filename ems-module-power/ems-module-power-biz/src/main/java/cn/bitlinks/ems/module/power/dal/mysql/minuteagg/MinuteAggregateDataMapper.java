package cn.bitlinks.ems.module.power.dal.mysql.minuteagg;

import cn.bitlinks.ems.framework.tenant.core.aop.TenantIgnore;
import cn.bitlinks.ems.module.power.controller.admin.report.electricity.vo.MinuteAggDataDTO;
import cn.bitlinks.ems.module.power.dal.dataobject.minuteagg.MinuteAggregateData;
import cn.bitlinks.ems.module.power.dal.dataobject.minuteagg.MinuteAggregateDataDO;
import cn.bitlinks.ems.module.power.dal.dataobject.minuteagg.SupplyWaterTmpMinuteAggData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 分钟聚合数据mapper
 */
@Mapper
public interface MinuteAggregateDataMapper {

    /**
     * 根据台账ID、参数编码和时间范围查询数据
     *
     * @param standingbookIds 台账ID列表
     * @param paramCodes      参数编码列表
     * @param startTime       开始时间
     * @param endTime         结束时间
     * @return 分钟聚合数据列表
     */
    @TenantIgnore
    List<MinuteAggregateDataDO> selectByStandingbookIdsAndParamCodes(
            @Param("standingbookIds") List<Long> standingbookIds,
            @Param("paramCodes") List<String> paramCodes,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * 根据台账ID、参数编码和日期区间查询当天最后一分钟的数据
     *
     * @param standingbookIds 台账ID列表
     * @param paramCodes      参数编码列表
     * @param startTime       开始时间
     * @param endTime         结束时间
     * @return 分钟聚合数据列表
     */
    @TenantIgnore
    List<MinuteAggregateDataDO> selectLastMinuteDataByDate(
            @Param("standingbookIds") List<Long> standingbookIds,
            @Param("paramCodes") List<String> paramCodes,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * 根据台账ID、参数编码和日期区间查询当天所有增量数据之和
     *
     * @param standingbookIds 台账ID列表
     * @param paramCodes      参数编码列表
     * @param startTime       开始时间
     * @param endTime         结束时间
     * @return 分钟聚合数据列表（已聚合）
     */
    @TenantIgnore
    List<MinuteAggregateDataDO> selectIncrementalSumByDate(
            @Param("standingbookIds") List<Long> standingbookIds,
            @Param("paramCodes") List<String> paramCodes,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * 根据台账和能源参数code获取温度聚合数据
     *
     * @param standingbookIds
     * @param paramCodes
     * @param starTime
     * @param endTime
     * @return
     */
    @TenantIgnore
    List<SupplyWaterTmpMinuteAggData> getTmpRangeDataSteady(@Param("standingbookIds") List<Long> standingbookIds,
                                                            @Param("paramCodes") List<String> paramCodes,
                                                            @Param("starTime") LocalDateTime starTime,
                                                            @Param("endTime") LocalDateTime endTime);

    /**
     * 批量查询最后一分钟数据 - 性能优化版本
     *
     * @param standingbookIds 台账ID列表
     * @param paramCodes      参数编码列表
     * @param startTime       开始时间
     * @param endTime         结束时间
     * @return 分钟聚合数据列表
     */
    @TenantIgnore
    List<MinuteAggregateDataDO> selectLastMinuteDataByDateBatch(
            @Param("standingbookIds") List<Long> standingbookIds,
            @Param("paramCodes") List<String> paramCodes,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * 批量查询增量数据之和 - 性能优化版本
     *
     * @param standingbookIds 台账ID列表
     * @param paramCodes      参数编码列表
     * @param startTime       开始时间
     * @param endTime         结束时间
     * @return 分钟聚合数据列表（已聚合）
     */
    @TenantIgnore
    List<MinuteAggregateDataDO> selectIncrementalSumByDateBatch(
            @Param("standingbookIds") List<Long> standingbookIds,
            @Param("paramCodes") List<String> paramCodes,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    @TenantIgnore
    LocalDateTime getLastTime(@Param("standingbookIds") List<Long> standingbookIds,
                              @Param("paramCodes") List<String> paramCodes,
                              @Param("starTime") LocalDateTime starTime,
                              @Param("endTime") LocalDateTime endTime);

    @TenantIgnore
    List<MinuteAggDataDTO> getMaxDataGpByDateType(@Param("standingbookIds") List<Long> standingbookIds, @Param("paramCodes") List<String> paramCodes, @Param("dateType") Integer dateType,
                                                  @Param("starTime") LocalDateTime starTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 根据台账 能源 参数 时间 获取数采数据
     *
     * @param standingbookId
     * @param paramCode
     * @param dateType
     * @param energyFlag
     * @param starTime
     * @param endTime
     * @return
     */
    @TenantIgnore
    List<MinuteAggregateData> getList(
            @Param("standingbookId") Long standingbookId,
            @Param("paramCode") String paramCode,
            @Param("dateType") Integer dateType,
            @Param("energyFlag") Integer energyFlag,
            @Param("dataFeature") Integer dataFeature,
            @Param("starTime") LocalDateTime starTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * 根据台账 能源 参数 时间 获取实时数采数据
     *
     * @param standingbookId
     * @param paramCode
     * @param energyFlag
     * @param starTime
     * @param endTime
     * @return
     */
    @TenantIgnore
    List<MinuteAggregateData> getRealTimeList(
            @Param("standingbookId") Long standingbookId,
            @Param("paramCode") String paramCode,
            @Param("energyFlag") Integer energyFlag,
            @Param("dataFeature") Integer dataFeature,
            @Param("starTime") LocalDateTime starTime,
            @Param("endTime") LocalDateTime endTime);
}

