package cn.bitlinks.ems.module.acquisition.api.job;

import cn.bitlinks.ems.module.acquisition.api.job.dto.AcquisitionJobDTO;
import cn.bitlinks.ems.module.acquisition.enums.ApiConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;


@FeignClient(name = ApiConstants.NAME) // TODO bitlinks：fallbackFactory =
@Tag(name = "RPC 服务 - 定时任务")
public interface QuartzApi {

    String PREFIX = ApiConstants.PREFIX + "/config";

    @PostMapping(PREFIX + "/createOrUpdateJob")
    @Operation(summary = "台账数采设置创建/修改/暂停/继续定时任务")
    void createOrUpdateJob(@RequestBody AcquisitionJobDTO acquisitionJobDTO);


    @GetMapping(PREFIX + "/deleteJob")
    @Operation(summary = "删除指定设备的数据采集任务")
    void deleteJob(@RequestParam Long standingbookId);

}