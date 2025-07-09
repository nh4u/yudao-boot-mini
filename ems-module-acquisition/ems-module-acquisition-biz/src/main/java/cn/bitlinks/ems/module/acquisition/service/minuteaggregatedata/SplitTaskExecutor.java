package cn.bitlinks.ems.module.acquisition.service.minuteaggregatedata;

import cn.bitlinks.ems.framework.common.util.json.JsonUtils;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.module.acquisition.api.collectrawdata.dto.MinuteAggDataSplitDTO;
import cn.bitlinks.ems.module.acquisition.api.collectrawdata.dto.MinuteAggregateDataDTO;
import cn.bitlinks.ems.module.acquisition.api.minuteaggregatedata.MinuteAggregateDataApi;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static cn.bitlinks.ems.module.acquisition.enums.CommonConstants.SPLIT_TASK_QUEUE_REDIS_KEY_PATTERN;

// 拆分任务 Redis 消费器
@Slf4j
@Component
public class SplitTaskExecutor {

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    @Resource
    private MinuteAggregateDataApi minuteAggregateDataApi;

    private final int scanKeyCount = 100;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    @PostConstruct
    public void start() {
        executor.scheduleWithFixedDelay(this::consumeAll, 5, 5, TimeUnit.SECONDS);
    }

    /**
     * 扫描匹配redis key
     *
     * @param pattern
     * @return
     */
    public Set<String> scanKeys(String pattern) {
        Set<String> keys = new HashSet<>();
        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(scanKeyCount).build();

        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                keys.add(cursor.next());
            }
        } catch (Exception e) {
            log.error("scan keys failed", e);
        }
        return keys;
    }

    public void consumeAll() {
        Set<String> keys = scanKeys(SPLIT_TASK_QUEUE_REDIS_KEY_PATTERN);
        for (String key : keys) {
            consumeOneQueue(key);
        }
    }

    private void consumeOneQueue(String queueKey) {
        while (true) {
            String json = redisTemplate.opsForList().rightPop(queueKey);
            if (StrUtil.isBlank(json)) break;

            try {
                MinuteAggDataSplitDTO dto = JsonUtils.parseObject(json, MinuteAggDataSplitDTO.class);
                insertByMinute(dto);
            } catch (Exception e) {
                log.error("拆分执行失败：{}", json, e);
                redisTemplate.opsForList().leftPush("split_task_queue:fail", json);
            }
        }
        redisTemplate.delete(queueKey);
    }

    /**
     * 包含两端数据
     *
     * @param dto
     */
    private void insertByMinute(MinuteAggDataSplitDTO dto) {
        LocalDateTime start = dto.getStartDataDO().getAggregateTime();
        LocalDateTime end = dto.getEndDataDO().getAggregateTime();

        long minutes = Duration.between(start, end).toMinutes();
        if (minutes < 0) return;
        if (minutes == 0) {
            minuteAggregateDataApi.insertDataBatch(Collections.singletonList(dto.getStartDataDO()));
            return;
        }

        BigDecimal total = dto.getEndDataDO().getFullValue().subtract(dto.getStartDataDO().getFullValue());
        BigDecimal perMin = total.divide(BigDecimal.valueOf(minutes), 10, RoundingMode.HALF_UP);

        List<MinuteAggregateDataDTO> dataList = new ArrayList<>();
        for (int i = 0; i <= minutes; i++) {
            MinuteAggregateDataDTO d = BeanUtils.toBean(dto.getStartDataDO(), MinuteAggregateDataDTO.class);
            d.setAggregateTime(start.plusMinutes(i));
            d.setFullValue(dto.getStartDataDO().getFullValue().add(perMin.multiply(BigDecimal.valueOf(i))));
            d.setIncrementalValue(perMin);
            dataList.add(d);
        }
        minuteAggregateDataApi.insertDataBatch(dataList);
    }
}