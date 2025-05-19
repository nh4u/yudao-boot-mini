package cn.bitlinks.ems.module.acquisition.api.collectrawdata;

import cn.bitlinks.ems.module.acquisition.api.collectrawdata.dto.CollectRawDataDTO;
import cn.bitlinks.ems.module.acquisition.enums.ApiConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;


@FeignClient(name = ApiConstants.NAME) // TODO bitlinks：fallbackFactory =
@Tag(name = "RPC 服务 - 数采-实时数据")
public interface CollectRawDataApi {

    String PREFIX = ApiConstants.PREFIX + "/collectRawData";

    @GetMapping(PREFIX + "/getCollectRawDataListByStandingBookIds")
    @Operation(summary = "删除指定设备的数据采集任务")
    List<CollectRawDataDTO> getCollectRawDataListByStandingBookIds(@RequestParam("standingBookIds") List<Long> standingBookIds);

}