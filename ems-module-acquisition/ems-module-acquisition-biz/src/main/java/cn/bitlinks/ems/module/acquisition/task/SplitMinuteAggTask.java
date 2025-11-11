package cn.bitlinks.ems.module.acquisition.task;


import cn.bitlinks.ems.framework.common.util.json.JsonUtils;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.framework.tenant.core.aop.TenantIgnore;
import cn.bitlinks.ems.module.acquisition.api.collectrawdata.dto.MinuteAggDataSplitDTO;
import cn.bitlinks.ems.module.acquisition.api.collectrawdata.dto.MinuteAggregateDataDTO;
import cn.bitlinks.ems.module.acquisition.service.minuteaggregatedata.MinuteAggregateDataService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.redisson.client.protocol.ScoredEntry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static cn.bitlinks.ems.module.acquisition.enums.CommonConstants.SPLIT_MINUTE_AGG_LOCK_KEY;
import static cn.bitlinks.ems.module.power.enums.CommonConstants.SPLIT_TASK_QUEUE_REDIS_KEY;

/**
 * 拆分小时级别数据 任务。
 */
@Slf4j
@Component
public class SplitMinuteAggTask {

    @Value("${spring.profiles.active}")
    private String env;

    @Resource
    private RedissonClient redissonClient;
    @Resource
    private MinuteAggregateDataService minuteAggregateDataService;

    // 单次调度最大执行时间（2秒，小于3秒的调度间隔）
    private static final long MAX_EXECUTE_MILLIS = 4000;

    // 每批最多处理数量（防止单次处理过多）
    @Value("${ems.batch-split-day-size}")
    private int batchSplitDaySize;

    @Scheduled(fixedDelay = 5000)
    @TenantIgnore
    public void scheduledSplitMinuteAggData() {
        String lockKey = String.format(SPLIT_MINUTE_AGG_LOCK_KEY, env);
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 尝试获取锁，高并发下快速跳过
            if (!lock.tryLock(5000, TimeUnit.MILLISECONDS)) {
                log.info("分钟聚合拆分任务 已由其他节点执行，跳过本次");
                return;
            }

            log.info("分钟聚合拆分任务 开始执行");
            long startTime = System.currentTimeMillis();

            String scoredSetKey = env + ":" + SPLIT_TASK_QUEUE_REDIS_KEY;
            RScoredSortedSet<String> scoredSet = redissonClient.getScoredSortedSet(scoredSetKey);

            while (!scoredSet.isEmpty()) {
                long elapsed = System.currentTimeMillis() - startTime;
                if (elapsed > MAX_EXECUTE_MILLIS) {
                    log.info("分钟聚合拆分任务 达到最大执行时间（{}ms），剩余任务下次处理", MAX_EXECUTE_MILLIS);
                    break;
                }

                // 每批处理 BATCH_SIZE 条
                Collection<ScoredEntry<String>> batch = scoredSet.entryRange(0, batchSplitDaySize - 1);
                if (batch.isEmpty()) {
                    break;
                }

                for (ScoredEntry<String> entry : batch) {
                    String json = entry.getValue(); // 获取元素
                    try {
                        MinuteAggDataSplitDTO dto = JsonUtils.parseObject(json, MinuteAggDataSplitDTO.class);
                        if (dto == null) {
                            log.warn("拆分任务反序列化为空，跳过 json: {}", json);
                            continue;
                        }

                        insertByMinute(dto);

                        // 成功处理后再从队列移除
                        scoredSet.remove(json);

                    } catch (Exception e) {
                        // 绝对安全策略：记录异常，但不删除队列任务
                        log.error("分钟聚合任务处理异常，保留任务以便下次执行 json: {}", json, e);
                    }

                    // 检查全局执行时间
                    if (System.currentTimeMillis() - startTime > MAX_EXECUTE_MILLIS) {
                        break;
                    }
                }

                // 每批处理后稍微休息，控制数据库压力
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ignored) {
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
            minuteAggregateDataService.insertDataBatch(Collections.singletonList(dto.getStartDataDO()));
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
        minuteAggregateDataService.insertDataBatch(dataList);
    }

}