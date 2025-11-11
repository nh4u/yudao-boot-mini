package cn.bitlinks.ems.module.acquisition.task;


import cn.bitlinks.ems.framework.common.util.json.JsonUtils;
import cn.bitlinks.ems.framework.tenant.core.aop.TenantIgnore;
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
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static cn.bitlinks.ems.module.acquisition.enums.CommonConstants.ACQ_MINUTE_AGG_LOCK_KEY;
import static cn.bitlinks.ems.module.power.enums.CommonConstants.ACQ_TASK_QUEUE_REDIS_KEY;

/**
 * 业务点入库
 */
@Slf4j
@Component
public class AcqMinuteAggTask {

    @Value("${spring.profiles.active}")
    private String env;

    @Resource
    private RedissonClient redissonClient;
    @Resource
    private MinuteAggregateDataService minuteAggregateDataService;

    // 单次调度最大执行时间（2秒，小于3秒的调度间隔）
    private static final long MAX_EXECUTE_MILLIS = 2000;

    // 每批最多处理数量（防止单次处理过多）
    @Value("${ems.batch-acq-size}")
    private int batchAcqSize;

    @Scheduled(fixedDelay = 3000)
    @TenantIgnore
    public void scheduledSplitMinuteAggData() {
        String lockKey = String.format(ACQ_MINUTE_AGG_LOCK_KEY, env);
        RLock lock = redissonClient.getLock(lockKey);

        try {
            log.info("尝试获取锁，锁键: {}", lockKey); // 日志：尝试获取锁
            // 尝试获取锁，高并发下快速跳过
            if (!lock.tryLock(5000, TimeUnit.MILLISECONDS)) {
                log.info("业务点入库任务 已由其他节点执行，跳过本次"); // 日志：锁竞争，跳过本次执行
                return;
            }

            log.info("业务点入库任务 开始执行");
            long startTime = System.currentTimeMillis();

            RScoredSortedSet<String> scoredSet = redissonClient.getScoredSortedSet(ACQ_TASK_QUEUE_REDIS_KEY);

            while (!scoredSet.isEmpty()) {
                long elapsed = System.currentTimeMillis() - startTime;
                if (elapsed > MAX_EXECUTE_MILLIS) {
                    log.info("业务点入库任务 达到最大执行时间（{}ms），剩余任务下次处理", MAX_EXECUTE_MILLIS);
                    break;
                }

                // 每批处理 BATCH_SIZE 条
                Collection<ScoredEntry<String>> batch = scoredSet.entryRange(0, batchAcqSize - 1);
                if (batch.isEmpty()) {
                    break;
                }

                for (ScoredEntry<String> entry : batch) {
                    String json = entry.getValue(); // 获取元素

                    try {
                        MinuteAggregateDataDTO dto = JsonUtils.parseObject(json, MinuteAggregateDataDTO.class);
                        if (dto == null) {
                            log.warn("业务点入库任务反序列化为空，跳过 json: {}", json);
                            continue;
                        }
                        // 入库
                        minuteAggregateDataService.insertDataBatch(Collections.singletonList(dto));

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
            log.error("业务点入库任务 发生异常", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }


}