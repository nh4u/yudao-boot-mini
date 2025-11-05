package cn.bitlinks.ems.module.power.service.starrocks;

import cn.bitlinks.ems.module.acquisition.api.collectrawdata.dto.MinuteAggregateDataDTO;
import cn.bitlinks.ems.module.acquisition.api.minuteaggregatedata.MinuteAggregateDataFiveMinuteApi;
import cn.hutool.core.collection.CollUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.ExecutorService;

import static cn.bitlinks.ems.module.power.enums.CommonConstants.BATCH_THRESHOLD;
import static cn.bitlinks.ems.module.power.enums.CommonConstants.REDIS_BATCH_QUEUE_KEY;

@Service
@Slf4j
public class StarRocksBatchImportService {

    // ===================== 常量配置 =====================

    @Value("${spring.profiles.active}")
    private String env;
    // ===================== 依赖注入 =====================
    @Resource
    private RedisTemplate<String, MinuteAggregateDataDTO> redisTemplate;

    @Resource
    private MinuteAggregateDataFiveMinuteApi minuteAggregateDataFiveMinuteApi;


    // 全局异步线程池
    @Resource(name = "starRocksAsyncExecutor")
    private ExecutorService starRocksAsyncExecutor;

    // ===================== 对外暴露的入队方法（其他地方只需要调用这个！） =====================

    /**
     * 数据入队：其他使用方只需调用这个方法，将数据放入Redis攒批队列
     * 无需关心攒批、导入逻辑，Service内部自动处理
     */
    public void addDataToQueue(List<MinuteAggregateDataDTO> dataList) {
        if (CollUtil.isEmpty(dataList)) {
            log.info("入队数据为空，跳过");
            return;
        }

        try {
            // 1. 批量存入Redis队列（左进右出，FIFO）
            redisTemplate.opsForList().leftPushAll(env + ":" + REDIS_BATCH_QUEUE_KEY, dataList);
            Long currentQueueSize = redisTemplate.opsForList().size(env + ":" + REDIS_BATCH_QUEUE_KEY);
            log.info("数据成功入队Redis，新增条数：{}，当前队列长度：{}", dataList.size(), currentQueueSize);

            // 2. 检查队列长度，够阈值则触发批量导入（异步执行，不阻塞当前线程）
            if (currentQueueSize != null && currentQueueSize >= BATCH_THRESHOLD) {
                triggerBatchImport();
            }
        } catch (Exception e) {
            log.error("数据入队Redis失败，", e);
        }
    }

    // ===================== 内部方法：触发批量导入 =====================

    /**
     * 从Redis队列取出BATCH_THRESHOLD条数据，异步调用Stream Load导入StarRocks
     */
    private void triggerBatchImport() {
        // 原子操作：从Redis队列右侧取出指定数量数据（避免多实例重复取）
        List<MinuteAggregateDataDTO> batchData = redisTemplate.opsForList().rightPop(env + ":" + REDIS_BATCH_QUEUE_KEY, BATCH_THRESHOLD);
        if (CollUtil.isEmpty(batchData)) {
            log.warn("Redis队列无足够数据，跳过批量导入");
            return;
        }

        log.info("触发StarRocks批量导入，数据量：{}", batchData.size());
        // 异步执行导入（复用全局线程池，限流并发）
        starRocksAsyncExecutor.submit(() -> {
            try {
                minuteAggregateDataFiveMinuteApi.insertDataBatch(batchData);
                log.info("StarRocks批量导入成功，数据量：{}", batchData.size());
            } catch (Exception e) {
                log.error("StarRocks批量导入失败，数据回滚Redis队列", e);
                // 失败回滚：数据重新放入Redis队列，等待下次攒批（最多重试3次）
                rollbackToRedisQueue(batchData);
            }
        });
    }

    // ===================== 内部辅助方法 =====================


    /**
     * 导入失败后，数据回滚到Redis队列（最多重试3次）
     */
    private void rollbackToRedisQueue(List<MinuteAggregateDataDTO> batchData) {
        int retryCount = 0;
        boolean rollbackSuccess = false;

        while (retryCount < 3 && !rollbackSuccess) {
            try {
                redisTemplate.opsForList().leftPushAll(env + ":" + REDIS_BATCH_QUEUE_KEY, batchData);
                rollbackSuccess = true;
                log.info("导入失败数据回滚Redis成功，重试次数：{}", retryCount);
            } catch (Exception e) {
                retryCount++;
                log.warn("导入失败数据回滚Redis失败，第{}次重试", retryCount, e);
                try {
                    Thread.sleep(1000L * retryCount); // 指数退避重试
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        // 3次回滚失败
        if (!rollbackSuccess) {
            log.error("导入失败数据回滚Redis3次失败");
        }
    }

    // ===================== 定时兜底任务（避免队列长期不满） =====================

    /**
     * 每10分钟执行一次：如果Redis队列有数据但没够阈值，主动触发导入
     * 避免数据长期堆积在Redis（比如最后一批只有5万条，一直不触发导入）
     */
    @Scheduled(fixedRate = 10 * 60 * 1000)
    public void flushQueuePeriodically() {
        Long queueSize = redisTemplate.opsForList().size(env + ":" + REDIS_BATCH_QUEUE_KEY);
        if (queueSize != null && queueSize > 0 && queueSize < BATCH_THRESHOLD) {
            log.info("定时任务触发：Redis队列数据量{}（不足{}条），主动导入", queueSize, BATCH_THRESHOLD);
            // 取出队列中所有数据，触发导入
            List<MinuteAggregateDataDTO> batchData = redisTemplate.opsForList().rightPop(env + ":" + REDIS_BATCH_QUEUE_KEY, queueSize.intValue());
            if (!CollUtil.isEmpty(batchData)) {
                starRocksAsyncExecutor.submit(() -> {
                    try {
                        log.info("定时任务导入StarRocks成功，数据量：{}", batchData.size());
                    } catch (Exception e) {
                        log.error("定时任务导入失败，数据回滚", e);
                        rollbackToRedisQueue(batchData);
                    }
                });
            }
        }
    }
}