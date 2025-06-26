package cn.bitlinks.ems.module.power.dal.mysql.copsettings;

import cn.bitlinks.ems.framework.mybatis.core.mapper.BaseMapperX;
import cn.bitlinks.ems.framework.tenant.core.aop.TenantIgnore;
import cn.bitlinks.ems.module.power.controller.admin.report.vo.CopHourAggData;
import cn.bitlinks.ems.module.power.dal.dataobject.copsettings.CopHourAggDataDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface CopHourAggDataMapper extends BaseMapperX<CopHourAggDataDO> {

    /**
     * 根据条件获取对应的COP 小时数据
     *
     * @param startDate
     * @param endDate
     * @param systemType
     * @return
     */
    @TenantIgnore
    List<CopHourAggData> getCopHourAggDataList(@Param("startDate") LocalDateTime startDate,
                                               @Param("endDate") LocalDateTime endDate,
                                               @Param("systemType") List<String> systemType);


    /**
     * 根据条件获取对应的COP 天数据
     *
     * @param startDate
     * @param endDate
     * @param systemType
     * @return
     */
    @TenantIgnore
    List<CopHourAggData> getCopDayAggDataList(@Param("startDate") LocalDateTime startDate,
                                              @Param("endDate") LocalDateTime endDate,
                                              @Param("systemType") List<String> systemType);

}