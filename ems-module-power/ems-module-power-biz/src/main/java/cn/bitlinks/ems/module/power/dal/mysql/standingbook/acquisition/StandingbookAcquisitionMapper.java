package cn.bitlinks.ems.module.power.dal.mysql.standingbook.acquisition;

import cn.bitlinks.ems.framework.mybatis.core.mapper.BaseMapperX;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.acquisition.StandingbookAcquisitionDO;
import cn.bitlinks.ems.module.power.dto.ServerParamsCacheDTO;
import cn.bitlinks.ems.module.power.dto.ServerStandingbookCacheDTO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 台账-数采设置 Mapper
 *
 * @author bitlinks
 */
@Mapper
public interface StandingbookAcquisitionMapper extends BaseMapperX<StandingbookAcquisitionDO> {

    /**
     * 查询缓存映射 每秒一次
     *
     * @return
     */
    List<ServerParamsCacheDTO> selectServerDataSiteMapping();

    /**
     * 查询缓存映射 每秒一次
     *
     * @return
     */
    List<ServerStandingbookCacheDTO> selectServerStandingbookMapping();
}