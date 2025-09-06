package cn.bitlinks.ems.module.power.service.collectrawdata;

import cn.bitlinks.ems.module.power.dal.dataobject.collectrawdata.CollectRawDataDO;

import java.util.List;

/**
 * 实时数据service
 */
public interface CollectRawDataService {


    /**
     * 查询最新的台账的采集数据
     *
     * @param dataSites 台账ids
     */
    List<CollectRawDataDO> getOutsideDataByDataSite(List<String> dataSites);


}
