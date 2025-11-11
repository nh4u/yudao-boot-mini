package cn.bitlinks.ems.module.power.task;

import cn.bitlinks.ems.module.acquisition.api.collectrawdata.dto.MinuteAggregateDataDTO;
import cn.bitlinks.ems.module.power.service.starrocks.StarRocksBatchImportService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

import static cn.bitlinks.ems.module.power.enums.CommonConstants.*;

@Component
@Slf4j
public class StarRocksQueueInspectTask {

    @Value("${spring.profiles.active}")
    private String env;

    @Resource
    private RedisTemplate<String, MinuteAggregateDataDTO> redisTemplate;
    @Resource
    private StarRocksBatchImportService starRocksBatchImportService; // 注入你的攒批Service
    @Resource
    private RedissonClient redissonClient;

    // 巡检间隔：5秒一次（可根据需求调整，比定时兜底短）
    private static final long INSPECT_INTERVAL = 5 * 1000;

    // 启动巡检线程（项目启动后自动运行）
    @PostConstruct
    public void startInspectThread() {
        new Thread(() -> {
            while (true) {
                try {
                    // 检查 quick=true 队列
                    inspectQueue();
                    // 巡检间隔：5秒
                    Thread.sleep(INSPECT_INTERVAL);
                } catch (InterruptedException e) {
                    log.error("巡检任务线程被中断", e);
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.error("巡检任务执行异常", e);
                    // 异常时也休眠，避免死循环
                    try {
                        Thread.sleep(INSPECT_INTERVAL);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }, "starrocks-queue-inspect-thread").start();
        log.info("StarRocks队列巡检任务启动成功，巡检间隔：{}ms", INSPECT_INTERVAL);
    }

    // 单个队列的巡检逻辑
    private void inspectQueue() {
        // 1. 获取队列配置（复用StarRocksBatchImportService的方法，需改为public或提供工具方法）
        String queueKey = starRocksBatchImportService.getQueueKeyByAcq();
        int threshold = starRocksBatchImportService.getThresholdByAcq();
        String redisKey = env + ":" + queueKey;
        String lockKey = String.format(STARROCKS_INSPECT_LOCK_KEY, env, queueKey);
        // 2. 加分布式锁：避免多实例重复巡检
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (!lock.tryLock(3000L, TimeUnit.MILLISECONDS)) {
                log.warn("quick=true 其他实例正在巡检，跳过");
                return;
            }
            Long queueSize = redisTemplate.opsForList().size(redisKey);
            // 检查条件：队列有数据且≥阈值
            if (queueSize != null && queueSize >= threshold) {
                log.info("巡检发现 quick=true 队列够阈值（当前：{}，阈值：{}），触发导入"
                        , queueSize, threshold);
                starRocksBatchImportService.triggerBatchImport();
            } else {
                log.debug("巡检 quick=true 队列：长度{}（不足阈值{}），跳过",
                         queueSize, threshold);
            }
        } catch (Exception e) {
            log.error("巡检任务 发生异常", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * acq=1 定时兜底：每1分钟执行（快速发送未达阈值数据）
     */
    @Scheduled(fixedRate = FLUSH_INTERVAL_QUICK)
    public void flushQueuePeriodicallyQuick() {
        String lockKey = String.format(STARROCKS_FLUSH_LOCK_KEY, env);
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (!lock.tryLock(3000L, TimeUnit.MILLISECONDS)) {
                log.warn("starocks批量快队列不满阈值触发 其他实例进行中，跳过");
                return;
            }
            starRocksBatchImportService.flushQueuePeriodically();
        } catch (Exception e) {
            log.error("starocks批量快队列不满阈值触发 发生异常", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }

    }



}