package cn.bitlinks.ems.module.acquisition.api.minuteaggregatedata;

import cn.bitlinks.ems.framework.common.exception.ServiceException;
import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.module.acquisition.api.collectrawdata.dto.MinuteAggDataSplitDTO;
import cn.bitlinks.ems.module.acquisition.api.collectrawdata.dto.MinuteAggregateDataDTO;
import cn.bitlinks.ems.module.acquisition.api.minuteaggregatedata.dto.MinuteRangeDataCopParamDTO;
import cn.bitlinks.ems.module.acquisition.api.minuteaggregatedata.dto.MinuteRangeDataParamDTO;
import cn.bitlinks.ems.module.acquisition.service.minuteaggregatedata.MinuteAggregateDataService;
import cn.hutool.core.collection.CollUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static cn.bitlinks.ems.module.acquisition.enums.ErrorCodeConstants.STREAM_LOAD_RANGE_FAIL;

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
    public CommonResult<String> insertSingleDataError(MinuteAggregateDataDTO minuteAggregateDataDTO) {
        try {
            minuteAggregateDataService.insertSingleData(minuteAggregateDataDTO);
            return CommonResult.success(null);
        } catch (ServiceException e) {
            return CommonResult.error(e);
        } catch (Exception e) {
            log.error("insertSingleDataError", e);
            return CommonResult.error(STREAM_LOAD_RANGE_FAIL);
        }
    }

    @Override
    public CommonResult<String> insertRangeDataError(MinuteAggDataSplitDTO minuteAggDataSplitDTO) {
        try {
            minuteAggregateDataService.insertRangeData(minuteAggDataSplitDTO);
            return CommonResult.success(null);
        } catch (ServiceException e) {
            return CommonResult.error(e);
        } catch (Exception e) {
            log.error("insertRangeDataError", e);
            return CommonResult.error(STREAM_LOAD_RANGE_FAIL);
        }
    }

    @Override
    public CommonResult<Map<Long, MinuteAggDataSplitDTO>> getPreAndNextData(MinuteRangeDataParamDTO minuteRangeDataParamDTO) {
        Map<Long, MinuteAggDataSplitDTO> dataSplitDTOMap = minuteAggregateDataService.getPreAndNextData(minuteRangeDataParamDTO);
        if (CollUtil.isEmpty(dataSplitDTOMap)) {
            return CommonResult.success(Collections.emptyMap());
        }
        // 否则，返回一个成功的CommonResult，其中包含列表
        return CommonResult.success(dataSplitDTOMap);
    }

    @Override
    public void insertRangeData(MinuteAggDataSplitDTO minuteAggDataSplitDTO) {
        minuteAggregateDataService.insertRangeData(minuteAggDataSplitDTO);
    }

    @Override
    public CommonResult<List<MinuteAggregateDataDTO>> getCopRangeData(MinuteRangeDataCopParamDTO minuteRangeDataCopParamDTO) {
        // 根据传入的参数，调用minuteAggregateDataService的getRangeDataRequestParam方法，获取MinuteAggregateDataDTO类型的列表
        List<MinuteAggregateDataDTO> list = minuteAggregateDataService.getCopRangeData(minuteRangeDataCopParamDTO.getSbIds(), minuteRangeDataCopParamDTO.getParamCodes(),minuteRangeDataCopParamDTO.getStarTime(), minuteRangeDataCopParamDTO.getEndTime());
        // 如果列表为空，则返回一个成功的CommonResult，其中包含null
        if (CollUtil.isEmpty(list)) {
            return CommonResult.success(null);
        }
        // 否则，返回一个成功的CommonResult，其中包含列表
        return CommonResult.success(list);
    }

    @Override
    public CommonResult<List<MinuteAggregateDataDTO>> getCopRangeDataSteady(MinuteRangeDataCopParamDTO minuteRangeDataCopParamDTO) {
        // 根据传入的参数，调用minuteAggregateDataService的getRangeDataRequestParam方法，获取MinuteAggregateDataDTO类型的列表
        List<MinuteAggregateDataDTO> list = minuteAggregateDataService.getCopRangeDataSteady(minuteRangeDataCopParamDTO.getSbIds(), minuteRangeDataCopParamDTO.getParamCodes(),minuteRangeDataCopParamDTO.getStarTime(), minuteRangeDataCopParamDTO.getEndTime());
        // 如果列表为空，则返回一个成功的CommonResult，其中包含null
        if (CollUtil.isEmpty(list)) {
            return CommonResult.success(null);
        }
        // 否则，返回一个成功的CommonResult，其中包含列表
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
