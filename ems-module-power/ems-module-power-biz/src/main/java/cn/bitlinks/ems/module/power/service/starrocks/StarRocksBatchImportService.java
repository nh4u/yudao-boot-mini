package cn.bitlinks.ems.module.power.service.starrocks;

import cn.bitlinks.ems.module.acquisition.api.collectrawdata.dto.MinuteAggregateDataDTO;
import cn.bitlinks.ems.module.acquisition.api.minuteaggregatedata.MinuteAggregateDataFiveMinuteApi;
import cn.hutool.core.collection.CollUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.ExecutorService;

import static cn.bitlinks.ems.module.power.enums.CommonConstants.*;

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
     * 按acq分队列入队
     *
     * @param dataList 数据列表（每条数据的acq标识需一致，避免混存）
     * @param quick
     */
    public void addDataToQueue(List<MinuteAggregateDataDTO> dataList, Boolean quick) {
        if (CollUtil.isEmpty(dataList)) {
            log.info("入队数据为空，跳过");
            return;
        }

        try {
            // 1. 根据acq选择对应的队列Key和阈值
            String queueKey = getQueueKeyByAcq(quick);

            // 2. 批量存入对应Redis队列（左进右出，FIFO）
            redisTemplate.opsForList().leftPushAll(env + ":" + queueKey, dataList);
        } catch (Exception e) {
            log.error("quick={} 数据入队Redis失败", quick, e);
        }
    }

    public String getQueueKeyByAcq(Boolean quick) {
        return quick ? REDIS_BATCH_QUEUE_KEY_QUICK : REDIS_BATCH_QUEUE_KEY;
    }

    public int getThresholdByAcq(Boolean quick) {
        return quick ? BATCH_THRESHOLD_QUICK : BATCH_THRESHOLD;
    }
    // ===================== 内部方法：触发批量导入 =====================

    /**
     * 从Redis队列取出BATCH_THRESHOLD条数据，异步调用Stream Load导入StarRocks
     */
    public void triggerBatchImport(Boolean quick) {
        String queueKey = getQueueKeyByAcq(quick);
        int threshold = getThresholdByAcq(quick);

        // 从对应队列取出指定数量数据
        List<MinuteAggregateDataDTO> batchData = redisTemplate.opsForList().rightPop(env + ":" + queueKey, threshold);
        if (CollUtil.isEmpty(batchData)) {
            log.warn("quick={} Redis队列无足够数据，跳过批量导入", quick);
            return;
        }

        log.info("quick={} 触发StarRocks批量导入，数据量：{}，队列Key：{}", quick, batchData.size(), queueKey);
        starRocksAsyncExecutor.submit(() -> {
            try {
                minuteAggregateDataFiveMinuteApi.insertDataBatch(batchData);
                log.info("quick={} StarRocks批量导入成功，数据量：{}", quick, batchData.size());
            } catch (Exception e) {
                log.error("quick={} StarRocks批量导入失败，数据回滚Redis队列", quick, e);
                rollbackToRedisQueue(batchData, quick); // 回滚到对应队列
            }
        });
    }


    // ===================== 内部辅助方法 =====================


    /**
     * 导入失败后，数据回滚到Redis队列（最多重试3次）
     */
    private void rollbackToRedisQueue(List<MinuteAggregateDataDTO> batchData, Boolean quick) {
        String queueKey = getQueueKeyByAcq(quick);
        int retryCount = 0;
        boolean rollbackSuccess = false;

        while (retryCount < 3 && !rollbackSuccess) {
            try {
                redisTemplate.opsForList().leftPushAll(env + ":" + queueKey, batchData);
                rollbackSuccess = true;
                log.info("quick={} 导入失败数据回滚Redis成功，重试次数：{}", quick, retryCount);
            } catch (Exception e) {
                retryCount++;
                log.warn("quick={} 导入失败数据回滚Redis失败，第{}次重试", quick, retryCount, e);
                try {
                    Thread.sleep(1000L * retryCount); // 指数退避
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        if (!rollbackSuccess) {
            log.error("quick={} 导入失败数据回滚Redis3次失败", quick);
        }
    }


    // ===================== 定时兜底任务（避免队列长期不满） =====================


    // 通用定时兜底逻辑（根据acq处理对应队列）
    public void flushQueuePeriodically(Boolean quick) {
        String queueKey = getQueueKeyByAcq(quick);
        int threshold = getThresholdByAcq(quick);
        Long queueSize = redisTemplate.opsForList().size(env + ":" + queueKey);

        if (queueSize != null && queueSize > 0 && queueSize < threshold) {
            log.info("quick={} 定时任务触发：Redis队列数据量{}（不足{}条），主动导入",
                    quick, queueSize, threshold);
            List<MinuteAggregateDataDTO> batchData = redisTemplate.opsForList().rightPop(env + ":" + queueKey, queueSize.intValue());
            if (!CollUtil.isEmpty(batchData)) {
                starRocksAsyncExecutor.submit(() -> {
                    try {
                        minuteAggregateDataFiveMinuteApi.insertDataBatch(batchData);
                        log.info("quick={} 定时任务导入StarRocks成功，数据量：{}", quick, batchData.size());
                    } catch (Exception e) {
                        log.error("quick={} 定时任务导入失败，数据回滚", quick, e);
                        rollbackToRedisQueue(batchData, quick);
                    }
                });
            }
        }
    }

}