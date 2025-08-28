package cn.bitlinks.ems.module.power.service.usagecost;

import cn.bitlinks.ems.module.power.controller.admin.report.electricity.vo.ConsumptionStatisticsParamVO;
import cn.bitlinks.ems.module.power.controller.admin.report.hvac.vo.BaseTimeDateParamVO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsParamV2VO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.UsageCostData;
import cn.bitlinks.ems.module.power.dto.UsageCostDTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @author wangl
 * @date 2025年05月13日 10:51
 */
public interface UsageCostService {
    List<UsageCostData> getList(StatisticsParamV2VO paramVO,
                                LocalDateTime startDate,
                                LocalDateTime endDate,
                                List<Long> standingBookIds);

    List<UsageCostData> getList(ConsumptionStatisticsParamVO paramVO,
                                LocalDateTime startDate,
                                LocalDateTime endDate,
                                List<Long> standingBookIds);

    List<UsageCostData> getList(Integer dateType,
                                LocalDateTime startDate,
                                LocalDateTime endDate,
                                List<Long> standingBookIds);
    List<UsageCostData> getList(LocalDateTime startDate,
                                LocalDateTime endDate,
                                List<Long> standingBookIds);

    LocalDateTime getLastTime(Integer dateType,
                              LocalDateTime startDate,
                              LocalDateTime endDate,
                              List<Long> standingBookIds);
    LocalDateTime getLastTime(StatisticsParamV2VO paramVO,
                              LocalDateTime startDate,
                              LocalDateTime endDate,
                              List<Long> standingBookIds);

    LocalDateTime getLastTime(ConsumptionStatisticsParamVO paramVO,
                              LocalDateTime startDate,
                              LocalDateTime endDate,
                              List<Long> standingBookIds);

    void saveList(List<UsageCostDTO> usageCostDOs);

    List<UsageCostData> getListOfHome(LocalDateTime startDate,
                                      LocalDateTime endDate,
                                      List<Long> energyIdList);


    List<UsageCostData> getEnergyAndSbStandardCoal(LocalDateTime startDate, LocalDateTime endDate, List<Long> standingBookIds);

    List<UsageCostData> getEnergyStandardCoal(LocalDateTime startDate, LocalDateTime endDate, List<Long> standingBookIds);

    List<UsageCostData> getEnergyStandardCoalByEnergyIds(LocalDateTime startDate, LocalDateTime endDate, List<Long> energyIds);
    BigDecimal getEnergySumStandardCoal(LocalDateTime startDate, LocalDateTime endDate, List<Long> energyIds);
    List<UsageCostData> getStandingbookStandardCoal(LocalDateTime startDate, LocalDateTime endDate, List<Long> standingBookIds);

    /**
     * 获取合计折标煤用量
     * @param startDate 开始时间
     * @param endDate 结束时间
     * @param standingBookIds 台账id
     * @return 总用量
     */
    BigDecimal getSumStandardCoal(LocalDateTime startDate, LocalDateTime endDate, List<Long> standingBookIds);
    List<UsageCostData> getEnergyUsage(Integer dateType, LocalDateTime startDate, LocalDateTime endDate, List<Long> standingBookIds);

    /**
     * 台账分组计算用量
     *
     * @param paramVO
     * @param startDate
     * @param endDate
     * @param longs
     * @return
     */
    List<UsageCostData> getUsageByStandingboookIdGroup(BaseTimeDateParamVO paramVO, LocalDateTime startDate, LocalDateTime endDate, List<Long> longs);

    LocalDateTime getLastTimeNoParam(
            LocalDateTime startDate,
            LocalDateTime endDate,
            List<Long> standingBookIds);
}
