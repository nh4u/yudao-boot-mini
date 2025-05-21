package cn.bitlinks.ems.module.acquisition.dal.mysql.collectrawdata;

import cn.bitlinks.ems.module.acquisition.dal.dataobject.collectrawdata.CollectRawDataDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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
            "calc_value, raw_value, collect_time ) VALUES " +
            "<foreach collection='details' item='detail' separator=','>" +
            "(#{detail.dataSite}, #{detail.syncTime}, #{detail.paramCode}, #{detail.energyFlag}, #{standingbookId}, " +
            "#{detail.calcValue}, #{detail.rawValue},#{detail.collectTime})" +
            "</foreach>" +
            "</script>")
    void insertBatch(@Param("standingbookId") Long standingbookId,
                     @Param("details") List<CollectRawDataDO> details);

    /**
     * 查询台账最新的采集数据
     *
     * @param standingbookId 台账id
     */

    List<CollectRawDataDO> selectLatestByStandingbookId(@Param("standingbookId") Long standingbookId);

    /**
     * 查询台账最新的采集数据
     * @param standingbookIds 台账ids
     */
    List<CollectRawDataDO> selectLatestByStandingbookIds(List<Long> standingbookIds);
}
