package cn.bitlinks.ems.module.acquisition.service.collectrawdata;

import cn.bitlinks.ems.module.acquisition.dal.mysql.collectrawdata.CollectRawDataMapper;
import com.baomidou.dynamic.datasource.annotation.DS;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;

/**
 * 实时数据service
 */
@DS("starrocks")
@Service
@Validated
public class CollectRawDataServiceImpl implements CollectRawDataService {

    @Resource
    private CollectRawDataMapper collectRawDataMapper;


}
