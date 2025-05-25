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
     * 批量插入采集详情
     *
     * @param standingbookId 台账ID
     * @param details        采集详情列表
     * @return 影响行数
     */
    @Insert("<script>" +
            "INSERT INTO collect_raw_data (data_site, sync_time, param_code, energy_flag, standingbook_id, " +
            "calc_value, raw_value, collect_time, usage) VALUES " +
            "<foreach collection='details' item='detail' separator=','>" +
            "(#{detail.dataSite}, #{detail.syncTime}, #{detail.paramCode}, #{detail.energyFlag}, #{standingbookId}, " +
            "#{detail.calcValue}, #{detail.rawValue},#{detail.collectTime},#{detail.usage})" +
            "</foreach>" +
            "</script>")
    @TenantIgnore
    void insertBatch(@Param("standingbookId") Long standingbookId,
                     @Param("details") List<CollectRawDataDO> details);

    /**
     * 查询台账最新的采集数据
     *
     * @param standingbookId 台账id
     */
    @TenantIgnore
    List<CollectRawDataDO> selectLatestByStandingbookId(@Param("standingbookId") Long standingbookId);

    /**
     * 查询台账最新的采集数据
     *
     * @param standingbookIds 台账ids
     */
    @TenantIgnore
    List<CollectRawDataDO> selectLatestByStandingbookIds(@Param("standingbookIds") List<Long> standingbookIds);

    /**
     * 获取最新的台账id
     */
    @TenantIgnore
    List<CollectRawDataDO> getGroupedData();

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
