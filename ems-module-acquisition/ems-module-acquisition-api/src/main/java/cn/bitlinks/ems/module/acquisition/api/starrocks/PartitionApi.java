package cn.bitlinks.ems.module.acquisition.api.starrocks;

import cn.bitlinks.ems.module.acquisition.enums.ApiConstants;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.security.PermitAll;
import java.time.LocalDateTime;

import static cn.bitlinks.ems.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@FeignClient(name = ApiConstants.NAME) // TODO bitlinks：fallbackFactory =
@Tag(name = "RPC 服务 - 分区")
public interface PartitionApi {
    String PREFIX = ApiConstants.PREFIX + "/partition";

    /**
     * 创建历史分区
     */
    @GetMapping(PREFIX + "/create")
    @PermitAll
    void createPartitions(@RequestParam("tableName") String tableName,
                          @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
                          @RequestParam("startDateTime") LocalDateTime startDateTime,
                          @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
                          @RequestParam("endDateTime") LocalDateTime endDateTime);
}
