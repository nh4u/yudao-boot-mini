package cn.bitlinks.ems.module.power.service.starrocks;

import cn.bitlinks.ems.framework.common.util.json.JsonUtils;
import cn.bitlinks.ems.module.acquisition.api.collectrawdata.dto.MinuteAggregateDataDTO;
import cn.bitlinks.ems.module.acquisition.api.minuteaggregatedata.MinuteAggregateDataFiveMinuteApi;
import cn.hutool.core.collection.CollUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
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
        int batchSize = 5000; // 每次处理5k条

        while (true) {
            // 如果剩余数据少于batchSize，就取剩余数量
            Long queueLen = redisTemplate.opsForList().size(env + ":" + queueKey);
            if (queueLen == null || queueLen == 0) break;

            int currentBatch = (int) Math.min(batchSize, queueLen);
            List<MinuteAggregateDataDTO> batchData = redisTemplate.opsForList()
                    .rightPop(env + ":" + queueKey, currentBatch);

            if (CollUtil.isEmpty(batchData)) break;

            log.info("quick={} 触发StarRocks批量导入，数据量：{}，队列Key：{}", quick, batchData.size(), queueKey);
            starRocksAsyncExecutor.submit(() -> {
                try {
                    minuteAggregateDataFiveMinuteApi.insertDataBatch(batchData);
                    log.info("quick={} StarRocks批量导入成功，数据量：{}", quick, batchData.size());
                } catch (Exception e) {
                    log.error("quick={} StarRocks批量导入失败，数据回滚Redis队列", quick, e);
                    rollbackToRedisQueue(batchData, quick);
                }
            });
        }
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
    /**
     * 通用定时兜底逻辑（根据acq处理对应队列）
     * 分批从Redis取数据（每次最多5000条）避免超时
     */
    public void flushQueuePeriodically(Boolean quick) {
        String queueKey = getQueueKeyByAcq(quick);
        int threshold = getThresholdByAcq(quick);
        String fullKey = env + ":" + queueKey;

        Long queueSize = redisTemplate.opsForList().size(fullKey);
        if (queueSize == null || queueSize == 0) {
            return;
        }

        if (queueSize < threshold) {
            log.info("quick={} 定时任务触发：Redis队列数据量 {}（不足 {} 条），主动导入",
                    quick, queueSize, threshold);

            List<MinuteAggregateDataDTO> allData = new ArrayList<>(queueSize.intValue());
            final int BATCH_POP_SIZE = 5000;
            long start = System.currentTimeMillis();

            while (allData.size() < queueSize) {
                int remain = queueSize.intValue() - allData.size();
                int fetch = Math.min(remain, BATCH_POP_SIZE);

                // 分批右出队
                List<MinuteAggregateDataDTO> popped = redisTemplate.opsForList().rightPop(fullKey, fetch);
                if (popped == null || popped.isEmpty()) {
                    break;
                }
                allData.addAll(popped);

                // 防止长时间阻塞
                if (System.currentTimeMillis() - start > 10000) {
                    log.warn("quick={} Redis分批pop超时，已取出 {} 条，提前结束", quick, allData.size());
                    break;
                }
            }

            if (CollUtil.isEmpty(allData)) {
                log.info("quick={} Redis队列已空或反序列化异常全部过滤，本次跳过", quick);
                return;
            }

            starRocksAsyncExecutor.submit(() -> {
                try {
                    minuteAggregateDataFiveMinuteApi.insertDataBatch(allData);
                    log.info("quick={} 定时任务导入StarRocks成功，数据量：{}", quick, allData.size());
                } catch (Exception e) {
                    log.error("quick={} 定时任务导入失败，数据回滚Redis队列", quick, e);
                    rollbackToRedisQueue(allData, quick);
                }
            });
        }
    }


}