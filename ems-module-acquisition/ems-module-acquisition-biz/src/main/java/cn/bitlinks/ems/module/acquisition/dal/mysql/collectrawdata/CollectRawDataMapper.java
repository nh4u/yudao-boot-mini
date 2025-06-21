package cn.bitlinks.ems.module.acquisition.dal.mysql.collectrawdata;

import cn.bitlinks.ems.framework.tenant.core.aop.TenantIgnore;
import cn.bitlinks.ems.module.acquisition.dal.dataobject.collectrawdata.CollectRawDataDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 实时数据mapper
 */
@Mapper
public interface CollectRawDataMapper {

    /**
     * 查询台账最新的采集数据
     *
     * @param standingbookIds 台账ids
     */
    @TenantIgnore
    List<CollectRawDataDO> selectLatestByStandingbookIds(@Param("standingbookIds") List<Long> standingbookIds);

    /**
     * 获取实时数据中的某一分钟的所有稳态值的末尾值
     */
    @TenantIgnore
    List<CollectRawDataDO> getGroupedSteadyFinalValue(@Param("startMinute") LocalDateTime startMinute,@Param("endMinute") LocalDateTime endMinute);
    /**
     * 获取实时数据中的稳态值参数
     */
    @TenantIgnore
    List<CollectRawDataDO> getGroupedStandingbookIdData();


    /**
     * 查询每个台账用量参数 当前时间点
     */
    @TenantIgnore
    List<CollectRawDataDO> selectExactDataBatch(@Param("standingbookIds") List<Long> standingbookIds, @Param("targetTime") LocalDateTime targetTime);

    /**
     * 查询每个台账用量参数 当前时间点之前的最新数据
     */
    @TenantIgnore
    List<CollectRawDataDO> selectPrevDataBatch(@Param("standingbookIds") List<Long> standingbookIds, @Param("targetTime") LocalDateTime targetTime);

    /**
     * 查询每个台账用量参数 当前时间点之后的第一条数据
     */
    @TenantIgnore
    List<CollectRawDataDO> selectNextDataBatch(@Param("standingbookIds") List<Long> standingbookIds, @Param("targetTime") LocalDateTime targetTime);

}
