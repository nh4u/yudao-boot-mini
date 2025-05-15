package cn.bitlinks.ems.module.power.dal.mysql.usagecost;

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
    LocalDateTime getLastTime(@Param("queryParam") StatisticsParamV2VO paramVO,
                                @Param("startDate") LocalDateTime startDate,
                                @Param("endDate") LocalDateTime endDate,
                                @Param("standingBookIds") List<Long> standingBookIds);

}
