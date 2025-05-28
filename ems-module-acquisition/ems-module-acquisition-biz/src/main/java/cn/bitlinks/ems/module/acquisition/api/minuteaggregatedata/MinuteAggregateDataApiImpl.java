package cn.bitlinks.ems.module.acquisition.api.minuteaggregatedata;

import cn.bitlinks.ems.module.acquisition.api.collectrawdata.dto.MinuteAggDataSplitDTO;
import cn.bitlinks.ems.module.acquisition.api.collectrawdata.dto.MinuteAggregateDataDTO;
import cn.bitlinks.ems.module.acquisition.service.minuteaggregatedata.MinuteAggregateDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.time.LocalDateTime;

@Slf4j
@RestController // 提供 RESTful API 接口，给 Feign 调用
@Validated
public class MinuteAggregateDataApiImpl implements MinuteAggregateDataApi {
    @Resource
    private MinuteAggregateDataService minuteAggregateDataService;

    @Override
    public MinuteAggregateDataDTO selectByAggTime(Long standingbookId, LocalDateTime thisCollectTime) {
        return minuteAggregateDataService.selectByAggTime(standingbookId, thisCollectTime);
    }

    @Override
    public MinuteAggregateDataDTO selectLatestByAggTime(Long standingbookId, LocalDateTime currentCollectTime) {
        return minuteAggregateDataService.selectLatestByAggTime(standingbookId, currentCollectTime);
    }

    @Override
    public MinuteAggregateDataDTO selectOldestByStandingBookId(Long standingbookId) {
        return minuteAggregateDataService.selectOldestByStandingBookId(standingbookId);
    }

    @Override
    public MinuteAggregateDataDTO selectLatestByStandingBookId(Long standingbookId) {
        return minuteAggregateDataService.selectLatestByStandingBookId(standingbookId);
    }

    @Override
    public void insertSingleData(MinuteAggregateDataDTO minuteAggregateDataDTO) {
        minuteAggregateDataService.insertSingleData(minuteAggregateDataDTO);
    }

    @Override
    public void insertDelRangeData(MinuteAggDataSplitDTO minuteAggDataSplitDTO) {
        minuteAggregateDataService.insertDelRangeData(minuteAggDataSplitDTO);
    }

    @Override
    public void insertRangeData(MinuteAggDataSplitDTO minuteAggDataSplitDTO) {
        minuteAggregateDataService.insertRangeData(minuteAggDataSplitDTO);
    }
}
