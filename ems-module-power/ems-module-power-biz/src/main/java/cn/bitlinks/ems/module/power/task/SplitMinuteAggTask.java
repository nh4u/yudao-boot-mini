package cn.bitlinks.ems.module.power.task;


import cn.bitlinks.ems.framework.common.util.json.JsonUtils;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.framework.tenant.core.aop.TenantIgnore;
import cn.bitlinks.ems.module.acquisition.api.collectrawdata.dto.MinuteAggDataSplitDTO;
import cn.bitlinks.ems.module.acquisition.api.collectrawdata.dto.MinuteAggregateDataDTO;
import cn.bitlinks.ems.module.power.service.starrocks.StarRocksBatchImportService;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static cn.bitlinks.ems.module.power.enums.CommonConstants.SPLIT_MINUTE_AGG_LOCK_KEY;
import static cn.bitlinks.ems.module.power.enums.CommonConstants.SPLIT_TASK_QUEUE_REDIS_KEY;

/**
 * 拆分小时级别数据 任务。3秒一次
 */
@Slf4j
@Component
public class SplitMinuteAggTask {

    @Value("${spring.profiles.active}")
    private String env;
    @Resource
    private RedissonClient redissonClient;

    @Resource
    private StarRocksBatchImportService starRocksBatchImportService;
    @Resource
    private RedisTemplate<String, String> redisTemplate;

    // 单次调度最大执行时间（2秒，小于3秒的调度间隔）
    private static final long MAX_EXECUTE_MILLIS = 2500;

    /**
     * 3秒拆分一批
     */
    @Scheduled(fixedDelay = 3000)
    @TenantIgnore
    public void scheduledSplitMinuteAggData() {
        String lockKey = String.format(SPLIT_MINUTE_AGG_LOCK_KEY, env);
        RLock lock = redissonClient.getLock(lockKey);

        try {
            if (!lock.tryLock(5000L, TimeUnit.MILLISECONDS)) {
                log.info("分钟聚合拆分任务 已由其他节点执行，跳过本次");
                return;
            }

            log.info("分钟聚合拆分任务 开始执行");
            long startTime = System.currentTimeMillis(); // 记录开始时间
            Boolean hasKey = redisTemplate.hasKey(SPLIT_TASK_QUEUE_REDIS_KEY);
            if (Boolean.FALSE.equals(hasKey)) {
                log.info("分钟聚合拆分任务 ZSET键不存在：{}", SPLIT_TASK_QUEUE_REDIS_KEY);
                return;
            }
            // 循环处理任务，直到超时或队列为空
            while (true) {
                //检查是否超过最大执行时间
                long elapsed = System.currentTimeMillis() - startTime;
                if (elapsed >= MAX_EXECUTE_MILLIS) {
                    log.info("分钟聚合拆分任务 单次调度已达最大执行时间（{}ms），剩余任务下次处理", MAX_EXECUTE_MILLIS);
                    break;
                }

                // 获取下一个任务
                ZSetOperations.TypedTuple<String> element = redisTemplate.opsForZSet().popMin(SPLIT_TASK_QUEUE_REDIS_KEY);
                if (element == null || StringUtils.isEmpty(element.getValue())) {
                    log.info("分钟聚合拆分任务 队列为空，本次处理结束");
                    break;
                }

                // 处理任务
                String json = element.getValue();
                try {
                    MinuteAggDataSplitDTO dto = JsonUtils.parseObject(json, MinuteAggDataSplitDTO.class);
                    insertByMinute(dto);
                } catch (Exception e) {
                    log.error("分钟聚合拆分任务 单词拆分失败 {} ，异常：", json, e);
                    if (json != null) {
                        ZonedDateTime zoned = LocalDateTime.now().atZone(ZoneId.systemDefault());
                        double score = zoned.toEpochSecond();
                        redisTemplate.opsForZSet().add(SPLIT_TASK_QUEUE_REDIS_KEY, json, score);
                    }
                }
            }

        } catch (Exception e) {
            log.error("分钟聚合拆分任务 发生异常", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
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
            starRocksBatchImportService.addDataToQueue(Collections.singletonList(dto.getStartDataDO()), false);
            return;
        }

        BigDecimal total = dto.getEndDataDO().getFullValue().subtract(dto.getStartDataDO().getFullValue());
        BigDecimal perMin = total.divide(BigDecimal.valueOf(minutes), 10, RoundingMode.HALF_UP);

        List<MinuteAggregateDataDTO> dataList = new ArrayList<>();
        for (int i = 0; i <= minutes; i++) {
            MinuteAggregateDataDTO d = BeanUtils.toBean(dto.getStartDataDO(), MinuteAggregateDataDTO.class);
            d.setAggregateTime(start.plusMinutes(i));
            d.setFullValue(dto.getStartDataDO().getFullValue().add(perMin.multiply(BigDecimal.valueOf(i))));
            // 增量不为负数
            d.setIncrementalValue(perMin.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : perMin);
            dataList.add(d);
        }
        starRocksBatchImportService.addDataToQueue(dataList, false);
    }


}