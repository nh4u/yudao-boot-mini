package cn.bitlinks.ems.module.acquisition.quartz.job;

import cn.bitlinks.ems.module.acquisition.quartz.entity.JobBean;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.Date;
import java.util.Objects;

/**
 *
 */
@Component
public class QuartzManager {

    @Autowired
    private Scheduler scheduler;


    /**
     * 增加一个job
     */
    public void createJob(JobBean jobBean) throws SchedulerException {
        JobDetail jobDetail = JobBuilder.newJob(jobBean.getJobClass()).withIdentity(jobBean.getJobName()).build();
        // 创建 Trigger，使用业务指定的开始时间
        TriggerBuilder<CronTrigger> triggerBuilder = TriggerBuilder.newTrigger()
                .withIdentity(jobBean.getJobName())
                .withSchedule(CronScheduleBuilder.cronSchedule(jobBean.getCronExpression()));

        // 设置开始时间（从 JobBean 获取）
        if (Objects.nonNull(jobBean.getStartTime())) {
            Date startDate = Date.from(jobBean.getStartTime().atZone(ZoneId.systemDefault()).toInstant());
            triggerBuilder.startAt(startDate);
        } else {
            triggerBuilder.startNow(); // 如果没有指定开始时间，默认立即启动
        }

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
        String jobName = jobBean.getJobName();
        // 定义 JobKey 和 TriggerKey
        JobKey jobKey = JobKey.jobKey(jobName);
        TriggerKey triggerKey = TriggerKey.triggerKey(jobName);

        // 检查 JobKey 是否存在
        if (!scheduler.checkExists(jobKey)) {
            throw new SchedulerException("Job with name " + jobName + " does not exist!");
        }

        // 检查 TriggerKey 是否存在
        if (!scheduler.checkExists(triggerKey)) {
            throw new SchedulerException("Trigger for job " + jobName + " does not exist!");
        }

        // 1. 更新 JobDetail（包含新的 JobDataMap）
        JobDetail newJobDetail = JobBuilder.newJob(AcquisitionJob.class)
                .withIdentity(jobKey)
                .usingJobData(jobBean.getJobDataMap())
                .build();

        // 替换旧的 JobDetail
        scheduler.addJob(newJobDetail, true);
        // 2. 比较和更新 Trigger（仅当 Cron 表达式不同时) 和开始时间不同时,更新触发器

        Trigger oldTrigger = scheduler.getTrigger(triggerKey);
        if (oldTrigger instanceof CronTrigger) {
            String oldCronExpression = ((CronTrigger) oldTrigger).getCronExpression();
            // 规范化比较（去除多余空格，统一大小写）
            if (normalizeCronExpression(oldCronExpression).equals(normalizeCronExpression(jobBean.getCronExpression()))
                    &&
                    oldTrigger.getStartTime().equals(Date.from(jobBean.getStartTime().atZone(ZoneId.systemDefault()).toInstant()))) {
                return;
            }
        }

        //创建并替换 Trigger
        TriggerBuilder<CronTrigger> triggerBuilder = TriggerBuilder.newTrigger()
                .withIdentity(triggerKey)
                .withSchedule(CronScheduleBuilder.cronSchedule(jobBean.getCronExpression()));
        // 设置开始时间（从 JobBean 获取）
        if (Objects.nonNull(jobBean.getStartTime())) {
            Date startDate = Date.from(jobBean.getStartTime().atZone(ZoneId.systemDefault()).toInstant());
            triggerBuilder.startAt(startDate);
        } else {
            triggerBuilder.startNow(); // 如果没有指定开始时间，默认立即启动
        }

        Trigger newTrigger = triggerBuilder.build();

        scheduler.rescheduleJob(triggerKey, newTrigger);
    }

    // 规范化 Cron 表达式以确保比较准确
    private String normalizeCronExpression(String cronExpression) {
        if (cronExpression == null) {
            return "";
        }
        // 去除多余空格，统一为小写（根据需要调整）
        return cronExpression.trim().replaceAll("\\s+", " ").toLowerCase();
    }

    /**
     * 删除任务一个job
     *
     * @param jobName 任务名称
     */
    public void deleteJob(String jobName) throws SchedulerException{
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

}
