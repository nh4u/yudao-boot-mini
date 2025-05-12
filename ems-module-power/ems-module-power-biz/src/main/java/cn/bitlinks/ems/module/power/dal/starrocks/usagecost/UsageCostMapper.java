package cn.bitlinks.ems.module.power.dal.starrocks.usagecost;

import org.apache.ibatis.annotations.Param;
import org.mapstruct.Mapper;

import java.time.LocalDateTime;
import java.util.List;

import cn.bitlinks.ems.framework.mybatis.core.mapper.BaseMapperX;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsParamV2VO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.UsageCostData;
import cn.bitlinks.ems.module.power.dal.dataobject.usagecost.UsageCostDO;

/**
 * @author wangl
 * @date 2025年05月09日 13:37
 */
@Mapper
public interface UsageCostMapper extends BaseMapperX<UsageCostDO> {


    List<UsageCostData> getList(@Param("queryParam") StatisticsParamV2VO paramVO,
                                @Param("startDate") LocalDateTime startDate,
                                @Param("endDate") LocalDateTime endDate,
                                @Param("standingBookIds") List<Long> standingBookIds);
}
