package cn.bitlinks.ems.module.power.task;


import cn.bitlinks.ems.framework.tenant.core.job.TenantJob;
import cn.bitlinks.ems.module.power.dal.dataobject.chemicals.PowerChemicalsSettingsDO;
import cn.bitlinks.ems.module.power.dal.mysql.chemicals.PowerChemicalsSettingsMapper;
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
 * 化学品录入
 *
 * @author liumingqiang
 */
@Slf4j
@Component
public class ChemicalsTask {

    @Value("${spring.profiles.active}")
    private String env;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private PowerChemicalsSettingsMapper powerChemicalsSettingsMapper;

    /**
     * 执行定时任务  每天凌晨1点
     * 2025-09-11 01:00:00
     * 2025-09-12 01:00:00
     */
    @Scheduled(cron = "0 0 1 * * ?")
    @TenantJob
    public void execute() {

        String LOCK_KEY = String.format(CHEMICALS_ADD_TASK_LOCK_KEY, env);

        RLock lock = redissonClient.getLock(LOCK_KEY);
        try {
            if (!lock.tryLock(5000L, TimeUnit.MILLISECONDS)) {
                log.info("化学品录入Task 已由其他节点执行");
                return;
            }
            try {
                log.info("化学品录入Task 开始");
                LocalDateTime time = LocalDateTimeUtil.beginOfDay(LocalDateTime.now());
                addTodayData(time);
                log.info("化学品录入Task 结束");
            } finally {
                lock.unlock();
            }

        } catch (Exception e) {
            log.error("化学品录入Task 执行失败", e);
        }


    }

    /**
     * 添加今日初始数据
     *
     * @param time
     */
    private void addTodayData(LocalDateTime time) {
        // 30%NAOH（氢氧化钠）
        PowerChemicalsSettingsDO naoh = new PowerChemicalsSettingsDO();
        naoh.setTime(time);
        naoh.setCode(NAOH);

        // 30%HCL（盐酸）
        PowerChemicalsSettingsDO hcl = new PowerChemicalsSettingsDO();
        hcl.setTime(time);
        hcl.setCode(HCL);

        powerChemicalsSettingsMapper.insert(naoh);
        powerChemicalsSettingsMapper.insert(hcl);
    }

}