package cn.bitlinks.ems.module.power.dal.mysql.collectrawdata;

import cn.bitlinks.ems.framework.tenant.core.aop.TenantIgnore;
import cn.bitlinks.ems.module.power.dal.dataobject.collectrawdata.CollectRawDataDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 实时数据mapper
 */
@Mapper
public interface CollectRawDataMapper {

    /**
     * 查询室外最新的采集数据
     *
     * @param dataSites 台账ids
     */
    @TenantIgnore
    List<CollectRawDataDO> getOutsideDataByDataSite(@Param("dataSites")List<String> dataSites);
}
