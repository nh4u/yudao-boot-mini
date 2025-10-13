package cn.bitlinks.ems.module.power.task;


import cn.bitlinks.ems.framework.tenant.core.job.TenantJob;
import cn.bitlinks.ems.module.power.dal.dataobject.chemicals.PowerChemicalsSettingsDO;
import cn.bitlinks.ems.module.power.dal.mysql.chemicals.PowerChemicalsSettingsMapper;
import cn.bitlinks.ems.module.power.service.sharefile.ShareFileSettingsService;
import cn.hutool.core.date.LocalDateTimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static cn.bitlinks.ems.module.power.enums.CommonConstants.*;

/**
 * 共享文件同步任务
 *
 * @author liumingqiang
 */
@Slf4j
@Component
public class ShareFileTask {

    @Value("${spring.profiles.active}")
    private String env;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private ShareFileSettingsService shareFileSettingsService;

    /**
     * 执行定时任务  每天中午12(0 0 12 * * ?)点执行昨天的文件 每分钟（0 0/1 * * * ?）
     * 2025-10-13 12:00:00
     * 2025-10-14 12:00:00
     * 2025-10-15 12:00:00
     * 2025-10-16 12:00:00
     * 2025-10-17 12:00:00
     */
    @Scheduled(cron = "0 0 12 * * ?")
    @TenantJob
    public void execute() {

        String LOCK_KEY = String.format(SHARE_FILE_TASK_LOCK_KEY, env);

        RLock lock = redissonClient.getLock(LOCK_KEY);
        try {
            if (!lock.tryLock(5000L, TimeUnit.MILLISECONDS)) {
                log.info("共享文件同步Task 已由其他节点执行");
                return;
            }
            try {
                log.info("共享文件同步Task 开始");
                shareFileSettingsService.dealFile();
                log.info("共享文件同步Task 结束");
            } finally {
                lock.unlock();
            }

        } catch (Exception e) {
            log.error("共享文件同步Task 执行失败", e);
        }


    }

}