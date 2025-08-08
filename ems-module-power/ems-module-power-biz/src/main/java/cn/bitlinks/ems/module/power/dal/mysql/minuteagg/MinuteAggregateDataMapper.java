package cn.bitlinks.ems.module.power.dal.mysql.minuteagg;

import cn.bitlinks.ems.framework.tenant.core.aop.TenantIgnore;
import cn.bitlinks.ems.module.power.dal.dataobject.minuteagg.MinuteAggregateDataDO;
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
     * 根据台账和能源参数code获取温度聚合数据
     *
     * @param standingbookIds
     * @param paramCodes
     * @param starTime
     * @param endTime
     * @return
     */
    @TenantIgnore
    List<MinuteAggregateDataDO> getTmpRangeDataSteady(@Param("standingbookIds") List<Long> standingbookIds,
                                                      @Param("paramCodes") List<String> paramCodes,
                                                      @Param("starTime") LocalDateTime starTime,
                                                      @Param("endTime") LocalDateTime endTime);
}

