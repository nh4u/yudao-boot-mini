package cn.bitlinks.ems.module.power.controller.admin.quartz.job;

import cn.bitlinks.ems.module.power.service.warningstrategy.WarningStrategyTriggerService;
import cn.hutool.core.date.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

import static cn.bitlinks.ems.module.acquisition.enums.CommonConstants.WARNING_STRATEGY_JOB_DATA_MAP_KEY_STRATEGY_ID;
import static cn.bitlinks.ems.module.power.enums.CommonConstants.STRATEGY_JOB_LOCK_KEY;

/**
 * 扫描策略
 */
@Slf4j
@Component
@PersistJobDataAfterExecution//让执行次数会递增
@DisallowConcurrentExecution//禁止并发执行
public class WarningStrategyJob implements Job {

    @Value("${spring.profiles.active}")
    private String env;
    @Resource
    private RedissonClient redissonClient;

    @Resource
    private WarningStrategyTriggerService warningStrategyTriggerService;

    public void execute(JobExecutionContext context) {
        String jobName = context.getJobDetail().getKey().getName();
        JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();
        String LOCK_KEY = String.format(STRATEGY_JOB_LOCK_KEY, env, jobName);
        RLock lock = redissonClient.getLock(LOCK_KEY);
        try {
            if (!lock.tryLock(5000L, TimeUnit.MICROSECONDS)) {
                log.info("策略任务Task {} 已由其他节点执行", jobName);
            }
            try {
                log.info("策略任务Task {} 开始执行", jobName);
                Long strategyId = (Long) jobDataMap.get(WARNING_STRATEGY_JOB_DATA_MAP_KEY_STRATEGY_ID);
                warningStrategyTriggerService.triggerWarning(strategyId, DateUtil.toLocalDateTime(context.getFireTime()));
                log.info("策略任务Task {} 执行完成", jobName);
            } finally {
                lock.unlock();
            }
        } catch (Exception e) {
            log.error("策略任务Task {} 执行失败", jobName, e);
        }
    }
}
