package cn.bitlinks.ems.module.power.dal.mysql.usagecost;

import cn.bitlinks.ems.module.power.controller.admin.report.electricity.vo.ConsumptionStatisticsParamVO;
import cn.bitlinks.ems.module.power.controller.admin.report.hvac.vo.BaseTimeDateParamVO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsParamV2VO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.UsageCostData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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

    List<UsageCostData> getTimeDataList(@Param("dateType") Integer dateType,
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
    List<UsageCostData> getListOfHome(@Param("startDate") LocalDateTime startDate,
                                      @Param("endDate") LocalDateTime endDate,
                                      @Param("energyIdList") List<Long> energyIdList);


    List<UsageCostData> getEnergyAndSbStandardCoal(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("standingBookIds") List<Long> standingBookIds);

    List<UsageCostData> getEnergyStandardCoal(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("standingBookIds") List<Long> standingBookIds);

    List<UsageCostData> getStandingbookStandardCoal(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("standingBookIds") List<Long> standingBookIds);

    List<UsageCostData> getEnergyStandardCoalByEnergyIds(@Param("startDate") LocalDateTime startDate,
                                                         @Param("endDate") LocalDateTime endDate,
                                                         @Param("energyIds") List<Long> energyIds);
    List<UsageCostData> getUsageByStandingboookIdGroup(@Param("queryParam") BaseTimeDateParamVO paramVO,
                                                    @Param("startDate")LocalDateTime startDate,
                                                  @Param("endDate")LocalDateTime endDate,
                                                  @Param("standingBookIds")List<Long> standingBookIds);
}
