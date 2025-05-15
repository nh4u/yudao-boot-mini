package cn.bitlinks.ems.module.acquisition.service.minuteaggregatedata;

import cn.bitlinks.ems.module.acquisition.dal.mysql.minuteaggregatedata.MinuteAggregateDataMapper;
import com.baomidou.dynamic.datasource.annotation.DS;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;

/**
 * 分钟聚合数据service
 */
@DS("starrocks")
@Service
@Validated
public class MinuteAggregateDataServiceImpl implements MinuteAggregateDataService {

    @Resource
    private MinuteAggregateDataMapper minuteAggregateDataMapper;


}
