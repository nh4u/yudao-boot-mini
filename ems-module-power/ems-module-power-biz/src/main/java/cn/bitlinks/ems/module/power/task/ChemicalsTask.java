package cn.bitlinks.ems.module.power.task;


import cn.bitlinks.ems.framework.tenant.core.job.TenantJob;
import cn.bitlinks.ems.module.power.dal.dataobject.chemicals.PowerChemicalsSettingsDO;
import cn.bitlinks.ems.module.power.dal.mysql.chemicals.PowerChemicalsSettingsMapper;
import cn.hutool.core.date.LocalDateTimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;

import static cn.bitlinks.ems.module.power.enums.CommonConstants.HCL;
import static cn.bitlinks.ems.module.power.enums.CommonConstants.NAOH;

/**
 * 化学品录入
 *
 * @author liumingqiang
 */
@Slf4j
@Component
public class ChemicalsTask {

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

        LocalDateTime time = LocalDateTimeUtil.beginOfDay(LocalDateTime.now());
        addTodayData(time);

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
        naoh.setSystem(NAOH);

        // 30%HCL（盐酸）
        PowerChemicalsSettingsDO hcl = new PowerChemicalsSettingsDO();
        hcl.setTime(time);
        hcl.setSystem(HCL);

        powerChemicalsSettingsMapper.insert(naoh);
        powerChemicalsSettingsMapper.insert(hcl);
    }

}