package cn.bitlinks.ems.module.power.task;


import cn.bitlinks.ems.framework.tenant.core.job.TenantJob;
import cn.bitlinks.ems.module.power.service.copsettings.CopCalcService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

import static cn.bitlinks.ems.module.power.enums.CommonConstants.COP_HOUR_AGG_TASK_LOCK_KEY;

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

    @Scheduled(cron = "0 20 * * * ?") // 每小时的20分钟时执行一次
    @TenantJob
    public void execute() {
        // 从聚合表中计算当前小时的值
        String LOCK_KEY = String.format(COP_HOUR_AGG_TASK_LOCK_KEY, env);

        RLock lock = redissonClient.getLock(LOCK_KEY);
        try {
            if (!lock.tryLock(5000L, TimeUnit.MICROSECONDS)) {
                log.info("COP HOUR 聚合任务Task 已由其他节点执行");
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
}