package cn.bitlinks.ems.module.acquisition.service.collectrawdata;

import cn.bitlinks.ems.module.acquisition.dal.dataobject.collectrawdata.CollectRawDataDO;

import java.util.List;

/**
 * 实时数据service
 */
public interface CollectRawDataService {


    /**
     * 查询最新的台账的采集数据
     *
     * @param standingbookIds 台账ids
     */
    List<CollectRawDataDO> selectLatestByStandingbookIds(List<Long> standingbookIds);


}
