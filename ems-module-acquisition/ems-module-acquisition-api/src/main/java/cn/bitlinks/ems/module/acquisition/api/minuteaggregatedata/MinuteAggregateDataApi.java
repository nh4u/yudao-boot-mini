package cn.bitlinks.ems.module.acquisition.api.minuteaggregatedata;

import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.module.acquisition.api.collectrawdata.dto.MinuteAggDataSplitDTO;
import cn.bitlinks.ems.module.acquisition.api.collectrawdata.dto.MinuteAggregateDataDTO;
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

import static cn.bitlinks.ems.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;


@FeignClient(name = ApiConstants.NAME) // TODO bitlinks：fallbackFactory =
@Tag(name = "RPC 服务 - 数采-聚合数据")
public interface MinuteAggregateDataApi {

    String PREFIX = ApiConstants.PREFIX + "/minuteAggregateData";

    /**
     * 获取当前数据
     *
     * @param thisCollectTime 当前采集时间点（分钟级别）
     * @return
     */
    @GetMapping(PREFIX + "/selectByAggTime")
    @Operation(summary = "查询设备指定时间的聚合数据")
    CommonResult<MinuteAggregateDataDTO> selectByAggTime(@RequestParam("standingbookId") Long standingbookId,
                                                         @RequestParam("thisCollectTime") @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND) LocalDateTime thisCollectTime);

    /**
     * 获取台账对应的最早的聚合数据
     *
     * @param standingbookId 台账id
     * @return
     */
    @GetMapping(PREFIX + "/selectOldestByStandingBookId")
    @Operation(summary = "获取台账对应的最早的聚合数据")
    CommonResult<MinuteAggregateDataDTO> selectOldestByStandingBookId(@RequestParam("standingbookId") Long standingbookId);

    /**
     * 获取台账对应的最新的聚合数据
     *
     * @param standingbookId 台账id
     * @return
     */
    @GetMapping(PREFIX + "/selectLatestByStandingBookId")
    @Operation(summary = "获取台账对应的最新的聚合数据")
    CommonResult<MinuteAggregateDataDTO> selectLatestByStandingBookId(@RequestParam("standingbookId") Long standingbookId);

    /**
     * 直接插入单条数据
     *
     * @param minuteAggregateDataDTO
     */
    @PostMapping(PREFIX + "/insertSingleData")
    @Operation(summary = "直接插入数据")
    void insertSingleData(@RequestBody MinuteAggregateDataDTO minuteAggregateDataDTO);

    /**
     * 根据两条数据进行拆分
     *
     * @param minuteAggDataSplitDTO
     */
    @PostMapping(PREFIX + "/insertRangeData")
    @Operation(summary = "根据两条数据进行拆分")
    void insertRangeData(@RequestBody MinuteAggDataSplitDTO minuteAggDataSplitDTO);


    /**
     * 获取台账们稳态值、用量的时间范围内数据
     *
     * @return
     */
    @PostMapping(PREFIX + "/getRangeDataRequestParam")
    @Operation(summary = "根据两条数据进行拆分")
    @PermitAll
    CommonResult<List<MinuteAggregateDataDTO>> getRangeDataRequestParam(@RequestBody MinuteRangeDataParamDTO minuteRangeDataParamDTO);
    /**
     * 获取该台账的当前业务点全量值
     * @param standingbookId
     * @param acquisitionTime
     * @return
     */
    @GetMapping(PREFIX + "/getUsageExistFullValue")
    @Operation(summary = "获取该台账的当前业务点全量值")
    MinuteAggregateDataDTO getUsageExistFullValue(@RequestParam("standingbookId") Long standingbookId,
                                                 @RequestParam("acquisitionTime")@DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND) LocalDateTime acquisitionTime);
    /**
     * 获取当前时间的上一个全量值
     * @param standingbookId 台账id
     * @param acquisitionTime 采集时间
     * @return
     */
    @GetMapping(PREFIX + "/getUsagePrevFullValue")
    @Operation(summary = "获取当前时间的上一个全量值")
    MinuteAggregateDataDTO getUsagePrevFullValue(@RequestParam("standingbookId") Long standingbookId,
                                                 @RequestParam("acquisitionTime")@DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND) LocalDateTime acquisitionTime);
    /**
     * 获取当前时间的下一个全量值
     * @param standingbookId 台账id
     * @param acquisitionTime 采集时间
     * @return
     */
    @GetMapping(PREFIX + "/getUsageNextFullValue")
    @Operation(summary = "获取当前时间的下一个全量值")
    MinuteAggregateDataDTO getUsageNextFullValue(@RequestParam("standingbookId") Long standingbookId,
                                                 @RequestParam("acquisitionTime")@DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND) LocalDateTime acquisitionTime);

}