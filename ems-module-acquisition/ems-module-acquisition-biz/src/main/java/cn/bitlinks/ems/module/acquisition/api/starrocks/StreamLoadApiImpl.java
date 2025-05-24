package cn.bitlinks.ems.module.acquisition.api.starrocks;

import cn.bitlinks.ems.module.acquisition.api.starrocks.dto.StreamLoadDTO;
import cn.bitlinks.ems.module.acquisition.starrocks.StarRocksStreamLoadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@Slf4j
@RestController // 提供 RESTful API 接口，给 Feign 调用
@Validated
public class StreamLoadApiImpl implements StreamLoadApi {
    @Resource
    private StarRocksStreamLoadService starRocksStreamLoadService;

    @Override
    public void streamLoadData(StreamLoadDTO streamLoadDTO) {
        try {
            starRocksStreamLoadService.streamLoadData(streamLoadDTO.getData(), streamLoadDTO.getLabel(), streamLoadDTO.getTableName());
        } catch (Exception e) {
            log.error("streamLoadData 批量新增失败：{}",e.getMessage(), e);
        }
    }
}
