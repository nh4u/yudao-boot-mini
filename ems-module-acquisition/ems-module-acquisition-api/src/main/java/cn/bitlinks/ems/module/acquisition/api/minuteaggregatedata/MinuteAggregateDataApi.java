package cn.bitlinks.ems.module.acquisition.api.minuteaggregatedata;

import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.module.acquisition.api.collectrawdata.dto.MinuteAggDataSplitDTO;
import cn.bitlinks.ems.module.acquisition.api.collectrawdata.dto.MinuteAggregateDataDTO;
import cn.bitlinks.ems.module.acquisition.api.collectrawdata.dto.MinutePrevExistNextDataDTO;
import cn.bitlinks.ems.module.acquisition.api.minuteaggregatedata.dto.MinuteRangeDataCopParamDTO;
import cn.bitlinks.ems.module.acquisition.api.minuteaggregatedata.dto.MinuteRangeDataParamDTO;
import cn.bitlinks.ems.module.acquisition.enums.ApiConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.security.PermitAll;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static cn.bitlinks.ems.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;


@FeignClient(name = ApiConstants.NAME) // TODO bitlinks：fallbackFactory =
@Tag(name = "RPC 服务 - 数采-聚合数据")
public interface MinuteAggregateDataApi {

    String PREFIX = ApiConstants.PREFIX + "/minuteAggregateData";


    @PostMapping(PREFIX + "/getPreAndNextData")
    @Operation(summary = "获取时间段首尾两端附近的数据")
    CommonResult<Map<Long, MinuteAggDataSplitDTO>> getPreAndNextData(@RequestBody MinuteRangeDataParamDTO minuteRangeDataParamDTO);

    /**
     * 获取台账们稳态值、用量的时间范围内数据
     *
     * @return
     */
    @PostMapping(PREFIX + "/getCopRangeData")
    @Operation(summary = "获取台账们、用量的时间范围内数据")
    @PermitAll
    CommonResult<List<MinuteAggregateDataDTO>> getCopRangeData(@RequestBody MinuteRangeDataCopParamDTO minuteRangeDataParamDTO);

    /**
     * 获取台账们稳态值、用量的时间范围内数据
     *
     * @return
     */
    @PostMapping(PREFIX + "/getCopRangeDataSteady")
    @Operation(summary = "获取台账们稳态值")
    @PermitAll
    CommonResult<List<MinuteAggregateDataDTO>> getCopRangeDataSteady(@RequestBody MinuteRangeDataCopParamDTO minuteRangeDataParamDTO);

    /**
     * 获取当前时间的上一个全量值
     *
     * @param standingbookId  台账id
     * @param acquisitionTime 采集时间
     * @return
     */
    @GetMapping(PREFIX + "/getUsagePrevFullValue")
    @Operation(summary = "获取当前时间的上一个全量值")
    MinuteAggregateDataDTO getUsagePrevFullValue(@RequestParam("standingbookId") Long standingbookId,
                                                 @RequestParam("acquisitionTime") @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND) LocalDateTime acquisitionTime);

    /**
     * 获取当前时间的下一个全量值
     *
     * @param standingbookId  台账id
     * @param acquisitionTime 采集时间
     * @return
     */
    @GetMapping(PREFIX + "/getUsageNextFullValue")
    @Operation(summary = "获取当前时间的下一个全量值")
    MinuteAggregateDataDTO getUsageNextFullValue(@RequestParam("standingbookId") Long standingbookId,
                                                 @RequestParam("acquisitionTime") @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND) LocalDateTime acquisitionTime);

    @GetMapping(PREFIX + "/getUsagePrevExistNextFullValue")
    @Operation(summary = "获取当前时间的三个全量值")
    MinutePrevExistNextDataDTO getUsagePrevExistNextFullValue(@RequestParam("standingbookId") Long standingbookId,
                                                              @RequestParam("acquisitionTime") @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND) LocalDateTime acquisitionTime);

}