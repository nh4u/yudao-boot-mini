package cn.bitlinks.ems.module.power.task;


import cn.bitlinks.ems.framework.tenant.core.job.TenantJob;
import cn.bitlinks.ems.module.power.service.cophouraggdata.RedisCopQueueService;
import cn.bitlinks.ems.module.power.service.copsettings.CopCalcService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

import static cn.bitlinks.ems.module.power.enums.CommonConstants.*;

/**
 * cop小时计算值的聚合计算任务
 */
@Slf4j
@Component
public class CopHourAggTask {

    @Value("${spring.profiles.active}")
    private String env;
    @Resource
    private RedissonClient redissonClient;

    @Resource
    private CopCalcService copCalcService;
    @Resource
    private RedisTemplate<String, String> redisTemplate;
    @Resource
    private RedisCopQueueService redisCopQueueService;
    // 单次调度最大执行时间（2秒，小于3秒的调度间隔）
    private static final long MAX_EXECUTE_MILLIS = 2000;

    @Scheduled(cron = "0 20 * * * ?") // 每小时的20分钟时执行一次
    @TenantJob
    public void execute() {
        // 从聚合表中计算当前小时的值
        String LOCK_KEY = String.format(COP_HOUR_AGG_TASK_LOCK_KEY, env);

        RLock lock = redissonClient.getLock(LOCK_KEY);
        try {
            if (!lock.tryLock(5000L, TimeUnit.MILLISECONDS)) {
                log.info("COP HOUR 聚合任务Task 已由其他节点执行");
                return;
            }
            try {
                log.info("COP HOUR 聚合任务Task 开始执行");
                LocalDateTime curTime = LocalDateTime.now();
                LocalDateTime startHour = curTime.truncatedTo(ChronoUnit.HOURS);
                LocalDateTime endHour = curTime.plusHours(1);
                copCalcService.calculateCop(startHour, endHour, null);
                log.info("COP HOUR 聚合任务Task 执行完成");
            } finally {
                lock.unlock();
            }
        } catch (Exception e) {
            log.error("COP HOUR 聚合任务Task 执行失败", e);
        }
    }

    /**
     * 3秒算一次
     */
    @Scheduled(fixedDelay = 3000)
    @TenantJob
    public void scheduleCopRecalc() {
        String lockKey = String.format(COP_HOUR_AGG_RECALC_TASK_LOCK_KEY, env);
        RLock lock = redissonClient.getLock(lockKey);

        try {
            if (!lock.tryLock(5000L, TimeUnit.MILLISECONDS)) {
                log.info("COP重算任务已由其他节点执行，跳过本次");
                return;
            }

            log.info("COP重算任务开始执行");
            // 记录开始时间
            long startTime = System.currentTimeMillis();
            // ✅ 1. 先检查队列是否存在
            Boolean hasKey = redisTemplate.hasKey(COP_RECALCULATE_HOUR_QUEUE);
            if (Boolean.FALSE.equals(hasKey)) {
                log.info("COP重算任务 ZSET键不存在：{}", COP_RECALCULATE_HOUR_QUEUE);
                return;
            }

            // ✅ 2. 再检查队列是否为空（ZCARD = 0 表示空）
            Long zcard = redisTemplate.opsForZSet().zCard(COP_RECALCULATE_HOUR_QUEUE);
            if (zcard == null || zcard == 0) {
                log.info("COP重算任务 队列为空（ZCARD=0），本次处理结束");
                return;
            }

            // 循环处理任务，直到超时或队列为空
            while (true) {
                // 检查是否超过最大执行时间
                long elapsed = System.currentTimeMillis() - startTime;
                if (elapsed >= MAX_EXECUTE_MILLIS) {
                    log.info("COP重算任务 单次调度已达最大执行时间（{}ms），剩余任务下次处理", MAX_EXECUTE_MILLIS);
                    break;
                }

                // ✅ 1. 先检查队列是否存在
                 hasKey = redisTemplate.hasKey(COP_RECALCULATE_HOUR_QUEUE);
                if (Boolean.FALSE.equals(hasKey)) {
                    log.info("COP重算任务 ZSET键不存在【while内】：{}", COP_RECALCULATE_HOUR_QUEUE);
                    break;
                }

                // ✅ 2. 再检查队列是否为空（ZCARD = 0 表示空）
                 zcard = redisTemplate.opsForZSet().zCard(COP_RECALCULATE_HOUR_QUEUE);
                if (zcard == null || zcard == 0) {
                    log.info("COP重算任务 队列为空（ZCARD=0），本次处理结束【while内】");
                    break;
                }

                // 获取下一个任务
                ZSetOperations.TypedTuple<String> element = redisTemplate.opsForZSet().popMin(COP_RECALCULATE_HOUR_QUEUE);
                if (element == null || element.getValue() == null) {
                    log.info("COP重算任务 队列为空，本次处理结束");
                    break;
                }

                // 处理任务
                String hourStr = element.getValue();
                LocalDateTime hourTime = null;
                try {
                    hourTime = LocalDateTime.parse(hourStr);
                    copCalcService.calculateCop(hourTime.minusHours(1), hourTime, null);
                } catch (Exception e) {
                    log.error("COP重算任务 单小时{} 处理失败：", hourStr, e);
                    if (hourTime != null) {
                        redisCopQueueService.pushHourForCopRecalc(hourTime);
                    }
                }
            }

        } catch (Exception e) {
            log.error("COP重算任务 发生异常", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }


}