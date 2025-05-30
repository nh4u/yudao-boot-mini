package cn.bitlinks.ems.module.power.task;


import cn.bitlinks.ems.framework.tenant.core.aop.TenantIgnore;
import cn.bitlinks.ems.framework.tenant.core.job.TenantJob;
import cn.bitlinks.ems.module.power.dal.dataobject.warningstrategy.WarningStrategyDO;
import cn.bitlinks.ems.module.power.service.warningstrategy.WarningStrategyService;
import cn.bitlinks.ems.module.power.service.warningstrategy.WarningStrategyTriggerService;
import cn.hutool.core.collection.CollUtil;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static cn.bitlinks.ems.module.power.enums.CommonConstants.STRATEGY_TASK_LOCK_KEY;

/**
 * 触发告警的任务
 */
@Slf4j
@Component
public class WarningTriggerTask {

    @Value("${spring.profiles.active}")
    private String env;
    @Resource
    private RedissonClient redissonClient;
    @Resource
    private WarningStrategyTriggerService warningStrategyTriggerService;
    @Resource
    private WarningStrategyService warningStrategyService;

    @Scheduled(cron = "0 0/1 * * * ? ") // 每分钟执行一次
    @TenantJob
    public void execute() {
        String LOCK_KEY = String.format(STRATEGY_TASK_LOCK_KEY, env);

        RLock lock = redissonClient.getLock(LOCK_KEY);
        try {
            if (!lock.tryLock(5000L, TimeUnit.MICROSECONDS)) {
                log.info("告警策略任务Task 已由其他节点执行");
            }
            try {
                log.info("告警策略任务Task 开始执行");
                LocalDateTime triggerTime = LocalDateTime.now();
                // 查询哪些条件未在间隔内被触发过，
                List<WarningStrategyDO> warningStrategyDOList =
                        warningStrategyService.queryNeedTriggerStrategyList(triggerTime);
                if (CollUtil.isEmpty(warningStrategyDOList)) {
                    return;
                }
                // 放入mq中
                warningStrategyTriggerService.triggerWarning(warningStrategyDOList, triggerTime);
                log.info("告警策略任务Task 执行完成");
            } finally {
                lock.unlock();
            }
        } catch (Exception e) {
            log.error("告警策略任务Task 执行失败", e);
        }
    }

}