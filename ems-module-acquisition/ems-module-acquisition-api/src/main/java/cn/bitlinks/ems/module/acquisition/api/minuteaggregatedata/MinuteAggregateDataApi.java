package cn.bitlinks.ems.module.acquisition.api.minuteaggregatedata;

import cn.bitlinks.ems.module.acquisition.api.collectrawdata.dto.MinuteAggDataSplitDTO;
import cn.bitlinks.ems.module.acquisition.api.collectrawdata.dto.MinuteAggregateDataDTO;
import cn.bitlinks.ems.module.acquisition.enums.ApiConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.time.LocalDateTime;


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
    MinuteAggregateDataDTO selectByAggTime(Long standingbookId, LocalDateTime thisCollectTime);

    /**
     * 获取当前时间上次的最新数据
     *
     * @param standingbookId     台账id
     * @param currentCollectTime 指定时间（分钟级别）
     * @return
     */
    @GetMapping(PREFIX + "/selectLatestByAggTime")
    @Operation(summary = "查询设备指定时间的上次聚合数据")
    MinuteAggregateDataDTO selectLatestByAggTime(Long standingbookId, LocalDateTime currentCollectTime);

    /**
     * 获取台账对应的最早的聚合数据
     *
     * @param standingbookId 台账id
     * @return
     */
    @GetMapping(PREFIX + "/selectOldestByStandingBookId")
    @Operation(summary = "获取台账对应的最早的聚合数据")
    MinuteAggregateDataDTO selectOldestByStandingBookId(Long standingbookId);

    /**
     * 获取台账对应的最新的聚合数据
     *
     * @param standingbookId 台账id
     * @return
     */
    @GetMapping(PREFIX + "/selectLatestByStandingBookId")
    @Operation(summary = "获取台账对应的最新的聚合数据")
    MinuteAggregateDataDTO selectLatestByStandingBookId(Long standingbookId);

    /**
     * 直接插入单条数据
     * @param minuteAggregateDataDTO
     */
    @PostMapping(PREFIX + "/insertSingleData")
    @Operation(summary = "直接插入数据")
    void insertSingleData(MinuteAggregateDataDTO minuteAggregateDataDTO);

    /**
     * 根据两条数据进行拆分，并且修改最后一条的增量
     * @param minuteAggDataSplitDTO
     */
    @PostMapping(PREFIX + "/insertDelRangeData")
    @Operation(summary = "根据两条数据进行拆分，并且修改最后一条的增量")
    void insertDelRangeData(MinuteAggDataSplitDTO minuteAggDataSplitDTO);

    /**
     * 根据两条数据进行拆分
     * @param minuteAggDataSplitDTO
     */
    @PostMapping(PREFIX + "/insertRangeData")
    @Operation(summary = "根据两条数据进行拆分")
    void insertRangeData(MinuteAggDataSplitDTO minuteAggDataSplitDTO);
}