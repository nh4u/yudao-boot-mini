package cn.bitlinks.ems.module.acquisition.service.collectrawdata;

import cn.bitlinks.ems.module.acquisition.dal.dataobject.collectrawdata.CollectRawDataDO;
import cn.bitlinks.ems.module.acquisition.dal.mysql.collectrawdata.CollectRawDataMapper;
import com.baomidou.dynamic.datasource.annotation.DS;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.List;

/**
 * 实时数据service
 */
@DS("starrocks")
@Service
@Validated
public class CollectRawDataServiceImpl implements CollectRawDataService {

    @Resource
    private CollectRawDataMapper collectRawDataMapper;


    @Override
    public void insertBatch(Long standingbookId, List<CollectRawDataDO> collectRawDataDOList) {
        collectRawDataMapper.insertBatch(standingbookId, collectRawDataDOList);
    }

    @Override
    public List<CollectRawDataDO> selectLatestByStandingbookId(Long standingbookId) {
        return collectRawDataMapper.selectLatestByStandingbookId(standingbookId);
    }

    @Override
    public List<CollectRawDataDO> selectLatestByStandingbookIds(List<Long> standingbookIds) {
        return collectRawDataMapper.selectLatestByStandingbookIds(standingbookIds);
    }


}
