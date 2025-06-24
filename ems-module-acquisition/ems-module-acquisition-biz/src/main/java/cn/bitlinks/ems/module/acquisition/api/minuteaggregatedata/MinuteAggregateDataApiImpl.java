package cn.bitlinks.ems.module.acquisition.api.minuteaggregatedata;

import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.module.acquisition.api.collectrawdata.dto.MinuteAggDataSplitDTO;
import cn.bitlinks.ems.module.acquisition.api.collectrawdata.dto.MinuteAggregateDataDTO;
import cn.bitlinks.ems.module.acquisition.service.minuteaggregatedata.MinuteAggregateDataService;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.StrPool;
import com.alibaba.cloud.commons.lang.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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
    public void insertSingleData(MinuteAggregateDataDTO minuteAggregateDataDTO) {
        minuteAggregateDataService.insertSingleData(minuteAggregateDataDTO);
    }

    @Override
    public void insertRangeData(MinuteAggDataSplitDTO minuteAggDataSplitDTO) {
        minuteAggregateDataService.insertRangeData(minuteAggDataSplitDTO);
    }

    @Override
    public CommonResult<List<MinuteAggregateDataDTO>> getRangeDataRequestParam(String standingbookIdsStr,LocalDateTime starTime, LocalDateTime endTime) {
        List<Long> standingbookIds = Collections.emptyList();
        if (StringUtils.isNotBlank(standingbookIdsStr)) {
            standingbookIds = Arrays.stream(standingbookIdsStr.split(StrPool.COMMA))
                    .map(Long::valueOf)
                    .collect(Collectors.toList());
        }
        List<MinuteAggregateDataDTO> list=  minuteAggregateDataService.getRangeDataRequestParam(standingbookIds, starTime, endTime);
        if (CollUtil.isEmpty(list)) {
            return CommonResult.success(null);
        }
        return CommonResult.success(list);
    }
    @Override
    public MinuteAggregateDataDTO getUsageExistFullValue(Long standingbookId, LocalDateTime acquisitionTime) {
        return minuteAggregateDataService.getUsageExistFullValue(standingbookId, acquisitionTime);
    }
    @Override
    public MinuteAggregateDataDTO getUsagePrevFullValue(Long standingbookId, LocalDateTime acquisitionTime) {
        return minuteAggregateDataService.getUsagePrevFullValue(standingbookId, acquisitionTime);
    }

    @Override
    public MinuteAggregateDataDTO getUsageNextFullValue(Long standingbookId, LocalDateTime acquisitionTime) {
        return minuteAggregateDataService.getUsageNextFullValue(standingbookId, acquisitionTime);
    }
}
