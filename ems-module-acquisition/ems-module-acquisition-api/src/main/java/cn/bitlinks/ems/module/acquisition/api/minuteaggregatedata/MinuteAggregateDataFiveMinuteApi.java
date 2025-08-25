package cn.bitlinks.ems.module.acquisition.api.minuteaggregatedata;

import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.module.acquisition.api.collectrawdata.dto.MinuteAggregateDataDTO;
import cn.bitlinks.ems.module.acquisition.api.config.ImportFeignConfig;
import cn.bitlinks.ems.module.acquisition.enums.ApiConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;


@FeignClient(name = ApiConstants.NAME,configuration = ImportFeignConfig.class) // TODO bitlinks：fallbackFactory =
@Tag(name = "RPC 服务 - 数采-聚合数据")
public interface MinuteAggregateDataFiveMinuteApi {

    String PREFIX = ApiConstants.PREFIX + "/minuteAggregateDataFiveMinute";

    /**
     * 直接插入数据(业务点数据)
     *
     * @param minuteAggregateDataDTO
     */
    @PostMapping(PREFIX + "/insertDataBatch")
    @Operation(summary = "直接插入数据")
    CommonResult<String> insertDataBatch(@RequestBody List<MinuteAggregateDataDTO> minuteAggregateDataDTO);


}