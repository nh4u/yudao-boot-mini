package cn.bitlinks.ems.module.power.task;

import cn.bitlinks.ems.framework.common.util.json.JsonUtils;
import cn.bitlinks.ems.framework.tenant.core.context.TenantContextHolder;
import cn.bitlinks.ems.module.power.controller.admin.doublecarbon.vo.DoubleCarbonSettingsRespVO;
import cn.bitlinks.ems.module.power.controller.admin.doublecarbon.vo.DoubleCarbonSettingsUpdVO;
import cn.bitlinks.ems.module.power.controller.admin.doublecarbon.vo.SyncDoubleCarbonData;
import cn.bitlinks.ems.module.power.service.doublecarbon.DoubleCarbonService;
import cn.bitlinks.ems.module.power.service.sync.SyncDoubleCarbonService;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;

/**
 * @Title: ydme-ems
 * @description:
 * @Author: Mingqiang LIU
 * @Date 2025/09/17 16:36
 **/

@Component
@EnableScheduling
@Slf4j
public class DynamicIntervalTaskScheduler implements SchedulingConfigurer {

    @Resource
    private DoubleCarbonService doubleCarbonService;

    @Resource
    private SyncDoubleCarbonService syncDoubleCarbonService;


    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        TenantContextHolder.setTenantId(1L);
        taskRegistrar.setScheduler(Executors.newScheduledThreadPool(1));

        taskRegistrar.addTriggerTask(
                // 任务逻辑
                () -> {
                    log.info("定时同步双碳服务-开始");
                    DoubleCarbonSettingsRespVO config = doubleCarbonService.getSettings();
                    LocalDateTime endTime = LocalDateTime.now();
                    LocalDateTime lastSyncTime = config.getLastSyncTime();
                    LocalDateTime startTime = Objects.isNull(lastSyncTime) ? endTime : lastSyncTime;
                    // 【startTime-1h,当前时间】
                    List<SyncDoubleCarbonData> list = syncDoubleCarbonService.getSyncDoubleCarbonData(endTime);
                    log.info("定时同步双碳服务-获取数据");
                    try {
                        log.info("定时同步双碳服务-发送请求");
                        // 构建POST请求 发送请求并获取响应
                        HttpResponse response = HttpRequest.post(config.getUrl())
                                .header(Header.CONTENT_TYPE, "application/json;charset=UTF-8")
                                // 超时，毫秒
                                .timeout(20000)
                                .body(JsonUtils.toJsonString(list)) // JSONUtil.toJsonStr(body)
                                .execute();

                        log.info("定时同步双碳服务请求状态" + response.getStatus());

                        // 把当前时间作为上次执行时间 更新上去
                        DoubleCarbonSettingsUpdVO doubleCarbonSettingsUpdVO = new DoubleCarbonSettingsUpdVO();
                        doubleCarbonSettingsUpdVO.setId(config.getId());
                        doubleCarbonSettingsUpdVO.setLastSyncTime(endTime);
                        doubleCarbonService.updLastSyncTime(doubleCarbonSettingsUpdVO);

                    } catch (Exception e) {
                        e.printStackTrace();

                        // 把当前时间作为上次执行时间 更新上去
                        DoubleCarbonSettingsUpdVO doubleCarbonSettingsUpdVO = new DoubleCarbonSettingsUpdVO();
                        doubleCarbonSettingsUpdVO.setId(config.getId());
                        doubleCarbonSettingsUpdVO.setLastSyncTime(lastSyncTime);
                        doubleCarbonService.updLastSyncTime(doubleCarbonSettingsUpdVO);
                    }
                    log.info("定时同步双碳服务-结束");
                },
                // Trigger：你实现的核心，用于动态计算下一次执行时间
                triggerContext -> {
                    // 每次调用时动态计算下一次执行时间
                    DoubleCarbonSettingsRespVO config = doubleCarbonService.getSettings();
                    return dealNextTime(config);
                }
        );
    }

    private Date dealNextTime(DoubleCarbonSettingsRespVO config) {

        Integer updateFrequency = config.getUpdateFrequency();
        Integer updateFrequencyUnit = config.getUpdateFrequencyUnit();

        LocalDateTime nextTime = null;
        LocalDateTime now = LocalDateTime.now();
        switch (updateFrequencyUnit) {
            case 1:
                nextTime = now.plusMinutes(updateFrequency);
                break;
            case 2:
                nextTime = now.plusHours(updateFrequency);
                break;
            case 3:
                nextTime = now.plusDays(updateFrequency);
                break;
            default:
                nextTime = now;
        }

        return Date.from(nextTime.atZone(ZoneId.systemDefault()).toInstant());
    }


}