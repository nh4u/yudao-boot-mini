package cn.bitlinks.ems.module.acquisition.quartz.job;

import cn.bitlinks.ems.module.acquisition.quartz.entity.JobBean;
import org.quartz.*;
import org.quartz.DateBuilder.IntervalUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
    public void addJob(JobBean jobBean) {
        try {
            JobDetail jobDetail = JobBuilder.newJob(jobBean.getJobClass()).withIdentity(jobBean.getJobName()).build();
            JobBuilder.newJob(jobBean.getJobClass()).withIdentity(jobBean.getJobName()).build();
            Trigger trigger = TriggerBuilder.newTrigger().withIdentity(jobBean.getJobName())
                    .startAt(DateBuilder.futureDate(1, IntervalUnit.SECOND))
                    .withSchedule(CronScheduleBuilder.cronSchedule(jobBean.getCronExpression())).startNow().build();

            scheduler.scheduleJob(jobDetail, trigger);
            if (!scheduler.isShutdown()) {
                scheduler.start();
            }
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }

    public void updateJob(JobBean jobBean) {
        try {
            TriggerKey triggerKey = TriggerKey.triggerKey(jobBean.getJobName());
            CronTrigger trigger = (CronTrigger) scheduler.getTrigger(triggerKey);
            trigger = trigger.getTriggerBuilder().withIdentity(triggerKey)
                    .withSchedule(CronScheduleBuilder.cronSchedule(jobBean.getCronExpression())).build();
            // 重启触发器
            scheduler.rescheduleJob(triggerKey, trigger);
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }

    /**
     * 删除任务一个job
     *
     * @param jobName 任务名称
     */
    public void deleteJob(String jobName) {
        try {
            scheduler.pauseTrigger(TriggerKey.triggerKey(jobName));
            scheduler.unscheduleJob(TriggerKey.triggerKey(jobName));
            scheduler.deleteJob(new JobKey(jobName));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 暂停一个job
     *
     * @param jobName 任务名称
     */
    public void pauseJob(String jobName) {
        try {

            JobKey jobKey = JobKey.jobKey(jobName);
            //scheduler.checkExists(jobKey);
            scheduler.pauseJob(jobKey);
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }

    /**
     * 恢复一个job
     *
     * @param jobName 任务名称
     */
    public void resumeJob(String jobName) {
        try {
            JobKey jobKey = JobKey.jobKey(jobName);
            scheduler.resumeJob(jobKey);
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }

}
