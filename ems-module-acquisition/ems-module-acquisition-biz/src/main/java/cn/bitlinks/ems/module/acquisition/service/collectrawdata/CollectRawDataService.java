package cn.bitlinks.ems.module.acquisition.service.collectrawdata;

import cn.bitlinks.ems.module.acquisition.dal.dataobject.collectrawdata.CollectRawDataDO;

import java.util.List;

/**
 * 实时数据service
 */
public interface CollectRawDataService {
    /**
     * 批量插入
     * @param standingbookId 台账id
     * @param collectRawDataDOList 实时数据列表
     */
    void insertBatch(Long standingbookId, List<CollectRawDataDO> collectRawDataDOList);

    /**
     * 查询最新的台账的采集数据
     * @param standingbookId 台账id
     */
    List<CollectRawDataDO> selectLatestByStandingbookId(Long standingbookId);


    /**
     * 查询最新的台账的采集数据
     * @param standingbookIds 台账ids
     */
    List<CollectRawDataDO> selectLatestByStandingbookIds(List<Long> standingbookIds);


}
