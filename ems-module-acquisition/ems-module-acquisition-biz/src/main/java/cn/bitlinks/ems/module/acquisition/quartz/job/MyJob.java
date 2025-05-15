package cn.bitlinks.ems.module.acquisition.quartz.job;

import org.quartz.*;

@PersistJobDataAfterExecution//让执行次数会递增
@DisallowConcurrentExecution//禁止并发执行
public class MyJob implements Job {

    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDetail jobDetail = context.getJobDetail();

//        System.out.println("任务名字：" + jobDetail.getKey().getName());
//        System.out.println("任务分组名字：" + jobDetail.getKey().getGroup());
//        System.out.println("任务类名字：" + jobDetail.getJobClass().getName());
        System.out.println("本次执行时间：" + context.getFireTime());
//        System.out.println("下次执行时间：" + context.getNextFireTime());

//        记录任务执行次数
        JobDataMap jobDataMap = jobDetail.getJobDataMap();
        Integer count = (Integer) jobDataMap.get("count");
        System.out.println("第" + count + "次执行");
        jobDataMap.put("count", ++count);
    }
}
