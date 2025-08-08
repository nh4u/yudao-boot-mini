package cn.bitlinks.ems.module.power.service.report.gas;

import cn.bitlinks.ems.module.power.dal.dataobject.minuteagg.MinuteAggregateDataDO;
import cn.bitlinks.ems.module.power.dal.mysql.minuteagg.MinuteAggregateDataMapper;
import com.baomidou.dynamic.datasource.annotation.DS;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 分钟聚合数据service
 */
@DS("starrocks")
@Service
@Validated
@Slf4j
public class MinuteAggregateDataServiceImpl implements MinuteAggregateDataService {

    @Resource
    private MinuteAggregateDataMapper minuteAggregateDataMapper;


    @Override
    public List<MinuteAggregateDataDO> selectByStandingbookIdsAndParamCodes(List<Long> standingbookIds, List<String> paramCodes, LocalDateTime startTime, LocalDateTime endTime) {
        return minuteAggregateDataMapper.selectByStandingbookIdsAndParamCodes(standingbookIds, paramCodes, startTime, endTime);
    }

    @Override
    public List<MinuteAggregateDataDO> selectLastMinuteDataByDate(List<Long> standingbookIds, List<String> paramCodes, LocalDateTime startTime, LocalDateTime endTime) {
        return minuteAggregateDataMapper.selectLastMinuteDataByDate(standingbookIds, paramCodes, startTime, endTime);
    }

    @Override
    public List<MinuteAggregateDataDO> selectIncrementalSumByDate(List<Long> standingbookIds, List<String> paramCodes, LocalDateTime startTime, LocalDateTime endTime) {
        return minuteAggregateDataMapper.selectIncrementalSumByDate(standingbookIds, paramCodes, startTime, endTime);
    }
}
