package cn.bitlinks.ems.module.acquisition.api.starrocks;

import cn.bitlinks.ems.module.acquisition.api.starrocks.dto.StreamLoadDTO;
import cn.bitlinks.ems.module.acquisition.enums.ApiConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import javax.annotation.security.PermitAll;

@FeignClient(name = ApiConstants.NAME) // TODO bitlinks：fallbackFactory =
@Tag(name = "RPC 服务 - 定时任务")
public interface StreamLoadApi {
    String PREFIX = ApiConstants.PREFIX + "/streamLoad";

    @PostMapping(PREFIX + "/insertBatch")
    @Operation(summary = "批量插入数据")
    @PermitAll
    void streamLoadData(@RequestBody StreamLoadDTO streamLoadDTO);
}
