//package cn.bitlinks.ems.module.power.quartz;
//
//import cn.bitlinks.ems.module.power.quartz.entity.JobBean;
//import org.quartz.*;
//import org.springframework.scheduling.quartz.QuartzJobBean;
//
//public class JobUtil {
//    /**
//     * 新建一个定时任务
//     *
//     * @param scheduler 调度器
//     * @param jobBean   任务名称
//     */
//    public static void createJob(Scheduler scheduler, JobBean jobBean) {
//        Class<? extends Job> jobClass = null;
//        JobDetail jobDetail = null;
//        Trigger trigger = null;
//        try {
//            jobClass = (Class<? extends Job>) Class.forName(jobBean.getJobClass());
//            jobDetail = JobBuilder.newJob(jobClass)
//                    .storeDurably()//持久化
//                    .withIdentity(jobBean.getJobName())
//                    .usingJobData("count", 1)//数据初始化。执行次数
//                    .build();
//            trigger = TriggerBuilder.newTrigger()
//                    .forJob(jobDetail)
//                    .withSchedule(CronScheduleBuilder.cronSchedule(jobBean.getCronExpression()))
//                    .withIdentity(jobBean.getJobName() + "_trigger1")
//                    .build();
//            scheduler.scheduleJob(jobDetail, trigger);
//        } catch (ClassNotFoundException e) {
//            e.printStackTrace();
//        } catch (SchedulerException e) {
//            e.printStackTrace();
//        }
//
//    }
//    /**
//     * 增加一个job
//     *
//     * @param jobClass     任务实现类
//     * @param jobName      任务名称
//     * @param jobGroupName 任务组名
//     * @param jobCron      cron表达式(如：0/5 * * * * ? )
//     */
//    public void addJob(Class<? extends Job> jobClass, String jobName, String jobGroupName, String jobCron) {
//        try {
//            JobDetail jobDetail = JobBuilder.newJob(jobClass).withIdentity(jobName, jobGroupName).build();
//            Trigger trigger = TriggerBuilder.newTrigger().withIdentity(jobName, jobGroupName)
//                    .startAt(DateBuilder.futureDate(1, DateBuilder.IntervalUnit.SECOND))
//                    .withSchedule(CronScheduleBuilder.cronSchedule(jobCron)).startNow().build();
//
//            sched.scheduleJob(jobDetail, trigger);
//            if (!sched.isShutdown()) {
//                sched.start();
//            }
//        } catch (SchedulerException e) {
//            e.printStackTrace();
//        }
//    }
//    /**
//     * 暂停定时任务
//     *
//     * @param scheduler 调度器
//     * @param jobBean   任务名称
//     */
//    public static void pauseJob(Scheduler scheduler, String jobBean) {
//        JobKey jobKey = JobKey.jobKey(jobBean);
//        try {
//            scheduler.pauseJob(jobKey);
//        } catch (SchedulerException e) {
//            e.printStackTrace();
//        }
//    }
//
//    /**
//     * 恢复定时任务
//     *
//     * @param scheduler
//     * @param jobBean
//     */
//    public static void resumeJob(Scheduler scheduler, String jobBean) {
//        JobKey jobKey = JobKey.jobKey(jobBean);
//        try {
//            scheduler.resumeJob(jobKey);
//        } catch (SchedulerException e) {
//            e.printStackTrace();
//        }
//    }
//
//    /**
//     * 删除定时任务
//     *
//     * @param scheduler
//     * @param jobBean
//     */
//    public static void deleteJob(Scheduler scheduler, String jobBean) {
//        JobKey jobKey = JobKey.jobKey(jobBean);
//        try {
//            scheduler.deleteJob(jobKey);
//        } catch (SchedulerException e) {
//            e.printStackTrace();
//        }
//    }
//
//    /**
//     * 任务执行一次
//     *
//     * @param scheduler
//     * @param jobName
//     */
//    public static void runJobOnce(Scheduler scheduler, String jobName) {
//        JobKey jobKey = new JobKey(jobName);
//        try {
//            scheduler.triggerJob(jobKey);
//        } catch (SchedulerException e) {
//            e.printStackTrace();
//        }
//    }
//
//    /**更修定时任务
//     * @param scheduler
//     * @param jobBean
//     */
//    public static void modifyJob(Scheduler scheduler, JobBean jobBean) {
//        //1.获取任务触发器的唯一标识
//        TriggerKey triggerKey = TriggerKey.triggerKey(jobBean.getJobName() + "_trigger1");//任务创建时给的触发器
//        //2.通过唯一标识获取旧的触发器对象
//        try {
//            CronTrigger oldTrigger = (CronTrigger) scheduler.getTrigger(triggerKey);
//            //3.使用cron表达式构建新的触发器
//            String newCron = jobBean.getCronExpression();
//           CronTrigger newTrigger = oldTrigger.getTriggerBuilder()
//                   .withSchedule(CronScheduleBuilder.cronSchedule(newCron).withMisfireHandlingInstructionDoNothing())//withMisfireHandlingInstructionDoNothing()用来解决misfire问题，没有这个内容，当更新了cron后会立即执行一次
//                   .build();
//           //4.调度器更新任务的触发器
//            scheduler.rescheduleJob(triggerKey,newTrigger);
//        } catch (SchedulerException e) {
//            e.printStackTrace();
//        }
//    }
//}
