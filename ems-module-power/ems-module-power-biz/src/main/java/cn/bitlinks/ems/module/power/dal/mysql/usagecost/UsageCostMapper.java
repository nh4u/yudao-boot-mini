package cn.bitlinks.ems.module.power.dal.mysql.usagecost;

import cn.bitlinks.ems.module.power.controller.admin.monitor.vo.DeviceMonitorAggData;
import cn.bitlinks.ems.module.power.controller.admin.report.electricity.vo.ConsumptionStatisticsParamVO;
import cn.bitlinks.ems.module.power.controller.admin.report.hvac.vo.BaseTimeDateParamVO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsHomeChartResultVO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsOverviewStatisticsTableData;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsParamV2VO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.UsageCostData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @author wangl
 * @date 2025年05月09日 13:37
 */
@Mapper
public interface UsageCostMapper {


    List<UsageCostData> getList(@Param("queryParam") StatisticsParamV2VO paramVO,
                                @Param("startDate") LocalDateTime startDate,
                                @Param("endDate") LocalDateTime endDate,
                                @Param("standingBookIds") List<Long> standingBookIds);

    List<UsageCostData> getList(@Param("queryParam") ConsumptionStatisticsParamVO paramVO,
                                @Param("startDate") LocalDateTime startDate,
                                @Param("endDate") LocalDateTime endDate,
                                @Param("standingBookIds") List<Long> standingBookIds);

    List<UsageCostData> getTimeSbUsageList(@Param("dateType") Integer dateType,
                                               @Param("startDate") LocalDateTime startDate,
                                               @Param("endDate") LocalDateTime endDate,
                                               @Param("standingBookIds") List<Long> standingBookIds);

    LocalDateTime getLastTime(@Param("dateType") Integer dateType,
                              @Param("startDate") LocalDateTime startDate,
                              @Param("endDate") LocalDateTime endDate,
                              @Param("standingBookIds") List<Long> standingBookIds);

    List<UsageCostData> getDataList(@Param("startDate") LocalDateTime startDate,
                                    @Param("endDate") LocalDateTime endDate,
                                    @Param("standingBookIds") List<Long> standingBookIds);

    LocalDateTime getLastTime(@Param("queryParam") StatisticsParamV2VO paramVO,
                              @Param("startDate") LocalDateTime startDate,
                              @Param("endDate") LocalDateTime endDate,
                              @Param("standingBookIds") List<Long> standingBookIds);

    LocalDateTime getLastTime(@Param("queryParam") ConsumptionStatisticsParamVO paramVO,
                              @Param("startDate") LocalDateTime startDate,
                              @Param("endDate") LocalDateTime endDate,
                              @Param("standingBookIds") List<Long> standingBookIds);

    LocalDateTime getLastTime2(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("standingBookIds") List<Long> standingBookIds);

    List<StatisticsHomeChartResultVO> getListOfHome(@Param("queryParam") StatisticsParamV2VO paramVO, @Param("startDate") LocalDateTime startDate,
                                                    @Param("endDate") LocalDateTime endDate,
                                                    @Param("energyIds") List<Long> energyIdList);
    List<StatisticsHomeChartResultVO> getListOfHomeBySbIds(@Param("queryParam") StatisticsParamV2VO paramVO,
                                                           @Param("startDate") LocalDateTime startDate,
                                                           @Param("endDate") LocalDateTime endDate,
                                                           @Param("standingBookIds") List<Long> energyIdList);

    List<UsageCostData> getEnergyAndSbStandardCoal(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("standingBookIds") List<Long> standingBookIds);

    List<UsageCostData> getEnergyStandardCoalCostBySbIds(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("standingBookIds") List<Long> standingBookIds);

    List<UsageCostData> getEnergyStandardCoalCostByEnergyIds(@Param("startDate") LocalDateTime startDate,
                                                         @Param("endDate") LocalDateTime endDate,
                                                         @Param("energyIds") List<Long> energyIds);
    List<UsageCostData> getStandingbookStandardCoal(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("standingBookIds") List<Long> standingBookIds);

    List<UsageCostData> getSbIdCostStandardCoal(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("standingBookIds") List<Long> standingBookIds);

    BigDecimal getEnergySumStandardCoal(@Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate,
                                        @Param("energyIds") List<Long> energyIds);

    List<UsageCostData> getUsageByStandingboookIdGroup(@Param("queryParam") BaseTimeDateParamVO paramVO,
                                                       @Param("startDate") LocalDateTime startDate,
                                                       @Param("endDate") LocalDateTime endDate,
                                                       @Param("standingBookIds") List<Long> standingBookIds);

    /**
     * 获取能源用量
     *
     * @param dateType
     * @param startDate
     * @param endDate
     * @param standingBookIds
     * @return
     */
    List<UsageCostData> getEnergyUsage(@Param("dateType") Integer dateType,
                                       @Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate,
                                       @Param("standingBookIds") List<Long> standingBookIds);

    /**
     * 获取能源用量
     *
     * @param dateType
     * @param startDate
     * @param endDate
     * @param energyIds
     * @return
     */
    List<UsageCostData> getTimeStandardCoalByEnergyIds(@Param("dateType") Integer dateType,
                                                    @Param("startDate") LocalDateTime startDate,
                                                    @Param("endDate") LocalDateTime endDate,
                                                    @Param("energyIds") List<Long> energyIds);

    /**
     * 获取能源用量
     *
     * @param dateType
     * @param startDate
     * @param endDate
     * @param energyIds
     * @return
     */
    List<UsageCostData> getEnergyUsageByEnergyIds(@Param("dateType") Integer dateType,
                                                  @Param("startDate") LocalDateTime startDate,
                                                  @Param("endDate") LocalDateTime endDate,
                                                  @Param("energyIds") List<Long> energyIds);

    /**
     * 获取能源用量
     *
     * @param dateType
     * @param startDate
     * @param endDate
     * @param standingBookIds
     * @return
     */
    List<UsageCostData> getEnergyUsageBySbIds(@Param("dateType") Integer dateType,
                                                  @Param("startDate") LocalDateTime startDate,
                                                  @Param("endDate") LocalDateTime endDate,
                                                  @Param("standingBookIds") List<Long> standingBookIds);

    /**
     * 获取折标煤总量
     */
    BigDecimal getSumStandardCoal(@Param("startDate") LocalDateTime startDate,
                                  @Param("endDate") LocalDateTime endDate,
                                  @Param("standingBookIds") List<Long> standingBookIds);

    /**
     * 根据台账ids获取时间分组折标煤
     *
     * @param dateType
     * @param startDate
     * @param endDate
     * @param standingBookIds
     * @return
     */
    List<UsageCostData> getTimeStandardCoalByStandardIds(@Param("dateType") Integer dateType,
                                                         @Param("startDate") LocalDateTime startDate,
                                                         @Param("endDate") LocalDateTime endDate,
                                                         @Param("standingBookIds") List<Long> standingBookIds);

    /**
     * 根据台账ids获取时间分组成本
     *
     * @param dateType
     * @param startDate
     * @param endDate
     * @param standingBookIds
     * @return
     */
    List<UsageCostData> getTimeCostByStandardIds(@Param("dateType") Integer dateType,
                                                 @Param("startDate") LocalDateTime startDate,
                                                 @Param("endDate") LocalDateTime endDate,
                                                 @Param("standingBookIds") List<Long> standingBookIds);

    StatisticsOverviewStatisticsTableData getAggStatisticsByEnergyIds(@Param("startDate") LocalDateTime startDate,
                                                                      @Param("endDate") LocalDateTime endDate,
                                                                      @Param("energyIds") List<Long> energyIds);
    StatisticsOverviewStatisticsTableData getAggStatisticsByStadingbookIds(@Param("startDate") LocalDateTime startDate,
                                                                      @Param("endDate") LocalDateTime endDate,
                                                                           @Param("sbIds") List<Long> sbIds);
    LocalDateTime getLastTimeByEnergyIds(@Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate,
                                         @Param("energyIds") List<Long> energyIds);

    DeviceMonitorAggData getAggStatisticsBySbIds(@Param("startDate") LocalDateTime startDate,
                                                 @Param("endDate") LocalDateTime endDate,
                                                 @Param("sbIds") List<Long> sbIds);

    /**
     * 获取 台账 时间 成本数据
     *
     * @param dateType
     * @param startDate
     * @param endDate
     * @param standingBookIds
     * @return
     */
    List<UsageCostData> getTimeSbCostList(@Param("dateType") Integer dateType,
                                          @Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate,
                                          @Param("standingBookIds") List<Long> standingBookIds);

}
