package cn.bitlinks.ems.module.acquisition.api.starrocks;

import cn.bitlinks.ems.module.acquisition.service.partition.PartitionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import java.time.LocalDateTime;

@Slf4j
@RestController // 提供 RESTful API 接口，给 Feign 调用
@Validated
@PermitAll
public class PartitionApiImpl implements PartitionApi {
    @Resource
    private PartitionService partitionService;

    @Override
    public void createPartitions(String tableName, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        partitionService.createPartitions(tableName, startDateTime, endDateTime);
    }

}
