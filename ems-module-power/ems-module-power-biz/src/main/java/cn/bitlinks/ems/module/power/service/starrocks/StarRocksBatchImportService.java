package cn.bitlinks.ems.module.power.service.starrocks;

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


    // ===================== 对外暴露的入队方法（其他地方只需要调用这个！） =====================

    /**
     * 按acq分队列入队
     *
     * @param dataList 数据列表（每条数据的acq标识需一致，避免混存）
     */
    public void addDataToQueue(List<MinuteAggregateDataDTO> dataList) {
        if (CollUtil.isEmpty(dataList)) {
            log.info("入队数据为空，跳过");
            return;
        }

        log.info("quick=true 入队数据量：{}", dataList.size());
        final int BATCH_SIZE = BATCH_POP_SIZE;
        try {
            String queueKey = getQueueKeyByAcq();
            String fullKey = env + ":" + queueKey;

            for (int i = 0; i < dataList.size(); i += BATCH_SIZE) {
                int end = Math.min(i + BATCH_SIZE, dataList.size());
                List<MinuteAggregateDataDTO> subList = dataList.subList(i, end);
                redisTemplate.opsForList().leftPushAll(fullKey, subList);
            }

        } catch (Exception e) {
            log.error("quick=true 数据入队Redis失败", e);
        }
    }

    public String getQueueKeyByAcq() {
        return REDIS_BATCH_QUEUE_KEY_QUICK;
    }

    public int getThresholdByAcq() {
        return BATCH_THRESHOLD_QUICK;
    }
    // ===================== 内部方法：触发批量导入 =====================

    /**
     * 从Redis队列取出BATCH_THRESHOLD条数据，异步调用Stream Load导入StarRocks
     */
    public void triggerBatchImport() {
        String queueKey = getQueueKeyByAcq();
        int threshold = getThresholdByAcq();
        String fullKey = env + ":" + queueKey;

        List<MinuteAggregateDataDTO> batchData = new ArrayList<>(threshold);

        while (batchData.size() < threshold) {
            int remaining = threshold - batchData.size();
            int currentPop = Math.min(BATCH_POP_SIZE, remaining);

            List<MinuteAggregateDataDTO> popped = redisTemplate.opsForList().rightPop(fullKey, currentPop);
            if (CollUtil.isEmpty(popped)) {
                break; // 队列空了
            }
            batchData.addAll(popped);
        }

        if (CollUtil.isEmpty(batchData)) {
            log.warn("quick=true Redis队列无足够数据，跳过批量导入");
            return;
        }

        log.info("quick=true 触发StarRocks批量导入，数据量：{}，队列Key：{}", batchData.size(), queueKey);
        // 分批提交给 StarRocks Feign，防止单次调用过大
        final int STARROCKS_BATCH_SIZE_FINAL = STARROCKS_BATCH_SIZE;
        for (int i = 0; i < batchData.size(); i += STARROCKS_BATCH_SIZE_FINAL) {
            int end = Math.min(i + STARROCKS_BATCH_SIZE_FINAL, batchData.size());
            List<MinuteAggregateDataDTO> subBatch = batchData.subList(i, end);
            try {
                minuteAggregateDataFiveMinuteApi.insertDataBatch(subBatch);
                log.info("quick=true StarRocks批量导入成功，数据量：{}", subBatch.size());
            } catch (Exception e) {
                log.error("quick=true StarRocks批量导入失败，数据回滚Redis队列", e);
                rollbackToRedisQueue(subBatch);
            }
        }

    }


    // ===================== 内部辅助方法 =====================


    /**
     * 导入失败后，数据回滚到Redis队列（最多重试3次）
     */
    private void rollbackToRedisQueue(List<MinuteAggregateDataDTO> batchData) {
        String queueKey = getQueueKeyByAcq();
        int retryCount = 0;
        boolean rollbackSuccess = false;

        while (retryCount < 3 && !rollbackSuccess) {
            try {
                redisTemplate.opsForList().leftPushAll(env + ":" + queueKey, batchData);
                rollbackSuccess = true;
                log.info("quick=true 导入失败数据回滚Redis成功，重试次数：{}", retryCount);
            } catch (Exception e) {
                retryCount++;
                log.warn("quick=true 导入失败数据回滚Redis失败，第{}次重试", retryCount, e);
                try {
                    Thread.sleep(1000L * retryCount); // 指数退避
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        if (!rollbackSuccess) {
            log.error("quick=true 导入失败数据回滚Redis3次失败");
        }
    }


    // ===================== 定时兜底任务（避免队列长期不满） =====================


    // 通用定时兜底逻辑（根据acq处理对应队列）

    /**
     * 通用定时兜底逻辑（根据acq处理对应队列）
     * 分批从Redis取数据（每次最多5000条）避免超时
     */
    public void flushQueuePeriodically() {
        String queueKey = getQueueKeyByAcq();
        int threshold = getThresholdByAcq();
        String fullKey = env + ":" + queueKey;

        Long queueSize = redisTemplate.opsForList().size(fullKey);
        if (queueSize == null || queueSize == 0) {
            return;
        }

        if (queueSize < threshold) {
            log.info("quick=true 定时任务触发：Redis队列数据量 {}（不足 {} 条），主动导入",
                    queueSize, threshold);

            List<MinuteAggregateDataDTO> allData = new ArrayList<>(queueSize.intValue());

            long start = System.currentTimeMillis();

            while (allData.size() < queueSize) {
                int remaining = queueSize.intValue() - allData.size();
                int currentPop = Math.min(BATCH_POP_SIZE, remaining);

                // 分批右出队
                List<MinuteAggregateDataDTO> popped = redisTemplate.opsForList().rightPop(fullKey, currentPop);
                if (CollUtil.isEmpty(popped)) {
                    break;
                }
                allData.addAll(popped);

                // 防止长时间阻塞
                if (System.currentTimeMillis() - start > 5000) {
                    log.warn("quick=true Redis分批pop超时，已取出 {} 条，提前结束", allData.size());
                    break;
                }
            }

            if (CollUtil.isEmpty(allData)) {
                log.info("quick=true Redis队列已空或反序列化异常全部过滤，本次跳过");
                return;
            }

            // 分批提交给 StarRocks Feign，防止单次调用过大
            final int STARROCKS_BATCH_SIZE_FINAL = STARROCKS_BATCH_SIZE;
            for (int i = 0; i < allData.size(); i += STARROCKS_BATCH_SIZE_FINAL) {
                int end = Math.min(i + STARROCKS_BATCH_SIZE_FINAL, allData.size());
                List<MinuteAggregateDataDTO> subBatch = allData.subList(i, end);
                try {
                    minuteAggregateDataFiveMinuteApi.insertDataBatch(subBatch);
                    log.info("quick=true 定时任务导入StarRocks成功，数据量：{}", subBatch.size());
                } catch (Exception e) {
                    log.error("quick=true 定时任务导入失败，数据回滚Redis队列", e);
                    rollbackToRedisQueue(subBatch);
                }
            }
        }
    }


}