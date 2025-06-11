package cn.bitlinks.ems.module.acquisition.api.minuteaggregatedata;

import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.module.acquisition.api.collectrawdata.dto.MinuteAggDataSplitDTO;
import cn.bitlinks.ems.module.acquisition.api.collectrawdata.dto.MinuteAggregateDataDTO;
import cn.bitlinks.ems.module.acquisition.service.minuteaggregatedata.MinuteAggregateDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Objects;

@Slf4j
@RestController // 提供 RESTful API 接口，给 Feign 调用
@Validated
public class MinuteAggregateDataApiImpl implements MinuteAggregateDataApi {
    @Resource
    private MinuteAggregateDataService minuteAggregateDataService;

    @Override
    public CommonResult<MinuteAggregateDataDTO> selectByAggTime(Long standingbookId, LocalDateTime thisCollectTime) {
        MinuteAggregateDataDTO minuteAggregateDataDTO = minuteAggregateDataService.selectByAggTime(standingbookId,
                thisCollectTime);
        if (Objects.isNull(minuteAggregateDataDTO)) {
            return CommonResult.success(null);
        }
        return CommonResult.success(minuteAggregateDataDTO);
    }

    @Override
    public CommonResult<MinuteAggregateDataDTO> selectLatestByAggTime(Long standingbookId, LocalDateTime currentCollectTime) {
        MinuteAggregateDataDTO minuteAggregateDataDTO = minuteAggregateDataService.selectLatestByAggTime(standingbookId, currentCollectTime);
        if (Objects.isNull(minuteAggregateDataDTO)) {
            return CommonResult.success(null);
        }
        return CommonResult.success(minuteAggregateDataDTO);
    }

    @Override
    public CommonResult<MinuteAggregateDataDTO> selectOldestByStandingBookId(Long standingbookId) {
        MinuteAggregateDataDTO minuteAggregateDataDTO = minuteAggregateDataService.selectOldestByStandingBookId(standingbookId);
        if (Objects.isNull(minuteAggregateDataDTO)) {
            return CommonResult.success(null);
        }
        return CommonResult.success(minuteAggregateDataDTO);
    }

    @Override
    public CommonResult<MinuteAggregateDataDTO> selectLatestByStandingBookId(Long standingbookId) {
        MinuteAggregateDataDTO minuteAggregateDataDTO = minuteAggregateDataService.selectLatestByStandingBookId(standingbookId);
        if (Objects.isNull(minuteAggregateDataDTO)) {
            return CommonResult.success(null);
        }
        return CommonResult.success(minuteAggregateDataDTO);
    }


    @Override
    public void insertRangeData(MinuteAggDataSplitDTO minuteAggDataSplitDTO) {
        minuteAggregateDataService.insertRangeData(minuteAggDataSplitDTO);
    }
}
