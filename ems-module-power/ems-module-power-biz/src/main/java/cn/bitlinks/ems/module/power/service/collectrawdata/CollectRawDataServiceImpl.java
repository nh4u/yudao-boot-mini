package cn.bitlinks.ems.module.power.service.collectrawdata;

import cn.bitlinks.ems.framework.tenant.core.aop.TenantIgnore;
import cn.bitlinks.ems.module.power.dal.dataobject.collectrawdata.CollectRawDataDO;
import cn.bitlinks.ems.module.power.dal.mysql.collectrawdata.CollectRawDataMapper;
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
    @TenantIgnore
    public List<CollectRawDataDO> getOutsideDataByDataSite(List<String> dataSites) {
        return collectRawDataMapper.getOutsideDataByDataSite(dataSites);
    }


}
