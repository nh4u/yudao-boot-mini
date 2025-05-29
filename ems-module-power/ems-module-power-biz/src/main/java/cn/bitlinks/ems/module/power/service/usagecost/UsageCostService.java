package cn.bitlinks.ems.module.power.service.usagecost;

import java.time.LocalDateTime;
import java.util.List;

import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsParamV2VO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.UsageCostData;
import cn.bitlinks.ems.module.power.dal.dataobject.usagecost.UsageCostDO;
import cn.bitlinks.ems.module.power.dto.UsageCostDTO;

/**
 * @author wangl
 * @date 2025年05月13日 10:51
 */
public interface UsageCostService {
    List<UsageCostData> getList(StatisticsParamV2VO paramVO,
                                LocalDateTime startDate,
                                LocalDateTime endDate,
                                List<Long> standingBookIds);

    LocalDateTime getLastTime(StatisticsParamV2VO paramVO,
                              LocalDateTime startDate,
                              LocalDateTime endDate,
                              List<Long> standingBookIds);

    void saveList(List<UsageCostDTO> usageCostDOs);

    List<UsageCostData> getListOfHome(LocalDateTime startDate,
                                      LocalDateTime endDate,
                                      List<Long> energyIdList);


    List<UsageCostData> getEnergyAndSbStandardCoal(LocalDateTime startDate, LocalDateTime endDate, List<Long> standingBookIds);

    List<UsageCostData> getEnergyStandardCoal(LocalDateTime startDate, LocalDateTime endDate, List<Long> standingBookIds);

    List<UsageCostData> getStandingbookStandardCoal(LocalDateTime startDate, LocalDateTime endDate, List<Long> standingBookIds);

}
