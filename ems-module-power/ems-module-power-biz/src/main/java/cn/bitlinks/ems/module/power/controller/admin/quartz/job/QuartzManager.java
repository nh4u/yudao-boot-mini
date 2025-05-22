package cn.bitlinks.ems.module.power.controller.admin.quartz.job;

import cn.bitlinks.ems.framework.common.enums.FrequencyUnitEnum;
import cn.bitlinks.ems.module.power.controller.admin.quartz.entity.JobBean;
import cn.hutool.core.collection.CollUtil;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

import static cn.bitlinks.ems.framework.common.enums.CommonConstants.SPRING_PROFILES_ACTIVE_LOCAL;

/**
 *
 */
@Component
public class QuartzManager {

    @Autowired
    private Scheduler scheduler;
    @Value("${spring.profiles.active}")
    private String env;

    /**
     * 增加一个job
     */
    public void createJob(JobBean jobBean) throws SchedulerException {
        JobDetail jobDetail = JobBuilder.newJob(jobBean.getJobClass())
                .withIdentity(jobBean.getJobName())
                .usingJobData(jobBean.getJobDataMap())
                .build();
        // 创建 Trigger，使用业务指定的开始时间
        TriggerBuilder<SimpleTrigger> triggerBuilder = TriggerBuilder.newTrigger()
                .withIdentity(jobBean.getJobName())
                .withSchedule(getSimpleSchedule(jobBean.getFrequency(), jobBean.getFrequencyUnit()));
        triggerBuilder.startNow(); // 如果没有指定开始时间，默认立即启动

        Trigger trigger = triggerBuilder.build();

        scheduler.scheduleJob(jobDetail, trigger);
        if (!scheduler.isShutdown()) {
            scheduler.start();
        }
    }

    /**
     * 获取触发器状态
     */
    public Trigger.TriggerState getTriggerState(String jobName) throws SchedulerException {
        TriggerKey triggerKey = TriggerKey.triggerKey(jobName);
        return scheduler.getTriggerState(triggerKey);
    }

    /**
     * 修改任务
     *
     * @param jobBean 任务
     */
    public void updateJob(JobBean jobBean) throws SchedulerException {
        deleteJob(jobBean.getJobName());
        createJob(jobBean);
    }

    /**
     * 删除任务一个job
     *
     * @param jobName 任务名称
     */
    public void deleteJob(String jobName) throws SchedulerException {
        scheduler.pauseTrigger(TriggerKey.triggerKey(jobName));
        scheduler.unscheduleJob(TriggerKey.triggerKey(jobName));
        scheduler.deleteJob(new JobKey(jobName));
    }

    /**
     * 判断任务是否存在
     *
     * @param jobName jobkey
     */
    public Boolean checkExists(String jobName) throws SchedulerException {
        JobKey jobKey = JobKey.jobKey(jobName);
        return scheduler.checkExists(jobKey);
    }

    /**
     * 暂停任务
     */
    public void pauseJob(String jobName) throws SchedulerException {
        JobKey jobKey = JobKey.jobKey(jobName);
        scheduler.pauseJob(jobKey);
    }

    /**
     * 恢复任务
     */
    public void resumeJob(String jobName) throws SchedulerException {
        JobKey jobKey = JobKey.jobKey(jobName);
        scheduler.resumeJob(jobKey);
    }

    // 定义 SimpleScheduleBuilder
    private SimpleScheduleBuilder getSimpleSchedule(long interval, Integer unit) {
        SimpleScheduleBuilder scheduleBuilder = SimpleScheduleBuilder.simpleSchedule();
        switch (FrequencyUnitEnum.codeOf(unit)) {
            case SECONDS:
                scheduleBuilder.withIntervalInSeconds(Math.toIntExact(interval));
                break;
            case MINUTES:
                scheduleBuilder.withIntervalInMinutes(Math.toIntExact(interval));
                break;
            case HOUR:
                scheduleBuilder.withIntervalInHours(Math.toIntExact(interval));
                break;
            case DAY:
                scheduleBuilder.withIntervalInHours(Math.toIntExact(interval * 24)); // 天转换为小时
                break;
            default:
                throw new IllegalArgumentException("Unsupported unit: " + unit);
        }

        // 设置重复次数，-1 表示无限重复
        scheduleBuilder.repeatForever();

        return scheduleBuilder;
    }


    /**
     * 批量修改任务的间隔
     *
     * @param interval     间隔
     * @param intervalUnit 间隔单位
     * @param jobNameList  任务名称列表
     */
    public void updateJobBatch(Integer interval, Integer intervalUnit, List<String> jobNameList) throws SchedulerException {
        if (CollUtil.isEmpty(jobNameList)) {
            // 遍历任务名称列表
            for (String jobName : jobNameList) {
                JobBean jobBean = new JobBean();
                jobBean.setJobName(jobName);
                jobBean.setFrequencyUnit(intervalUnit);
                jobBean.setFrequency(interval);
                updateJob(jobBean);
            }
        }
    }

    public void init() throws SchedulerException, InterruptedException {
        // 本地环境不初始化
        if (SPRING_PROFILES_ACTIVE_LOCAL.equals(env)) {
            return;
        }
        Thread.sleep(5000L);
        scheduler.start();
    }
}
