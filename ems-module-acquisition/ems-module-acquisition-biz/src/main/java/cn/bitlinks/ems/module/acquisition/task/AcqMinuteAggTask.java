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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
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
            log.info("尝试获取锁，锁键: {}", lockKey);
            if (!lock.tryLock(5000, TimeUnit.MILLISECONDS)) {
                log.info("业务点入库任务 已由其他节点执行，跳过本次");
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

                // 每批处理 batchAcqSize 条
                Collection<ScoredEntry<String>> batch = scoredSet.entryRange(0, batchAcqSize - 1);
                if (batch.isEmpty()) {
                    log.info("业务点入库任务 当前队列为空，本次处理结束");
                    break;
                }

                List<MinuteAggregateDataDTO> batchList = new ArrayList<>();
                List<String> successfulJsons = new ArrayList<>();

                for (ScoredEntry<String> entry : batch) {
                    String json = entry.getValue();
                    try {
                        MinuteAggregateDataDTO dto = JsonUtils.parseObject(json, MinuteAggregateDataDTO.class);
                        if (dto != null) {
                            batchList.add(dto);
                            successfulJsons.add(json); // 成功处理后再删除
                        } else {
                            log.warn("业务点入库任务反序列化为空，跳过 json: {}", json);
                        }
                    } catch (Exception e) {
                        log.error("业务点入库任务反序列化异常，保留任务 json: {}", json, e);
                    }
                }

                // 批量入库
                if (!batchList.isEmpty()) {
                    try {
                        minuteAggregateDataService.insertDataBatch(batchList);
                        // 入库成功，移除 Redis
                        for (String json : successfulJsons) {
                            scoredSet.remove(json);
                        }
                        log.info("业务点入库任务 批量处理成功，本批次条数: {}", batchList.size());
                    } catch (Exception e) {
                        log.error("业务点入库任务 批量入库失败，保留任务以便下次重试", e);
                    }
                }

                // 控制数据库压力
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ignored) {}
            }

        } catch (Exception e) {
            log.error("业务点入库任务 发生异常", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
            log.info("业务点入库任务 执行结束");
        }
    }



}