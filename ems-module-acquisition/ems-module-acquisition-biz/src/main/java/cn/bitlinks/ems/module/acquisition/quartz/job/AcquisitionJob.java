package cn.bitlinks.ems.module.acquisition.quartz.job;

import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 数据采集定时任务
 */
@Component
@PersistJobDataAfterExecution//让执行次数会递增
@DisallowConcurrentExecution//禁止并发执行
public class AcquisitionJob implements Job {

    @Autowired
    private RocketMQTemplate rocketmqTemplate;
    @Value("${rocketmq.topic.device-task}")
    private String deviceTaskTopic;

    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDetail jobDetail = context.getJobDetail();

//        System.out.println("任务名字：" + jobDetail.getKey().getName());
//        System.out.println("任务分组名字：" + jobDetail.getKey().getGroup());
//        System.out.println("任务类名字：" + jobDetail.getJobClass().getName());
        // 根据传递的设备和设备参数，进行数据采集，

        // 调用采集接口
        // 采集到的数据转到mq进行计算


        System.out.println("本次执行时间：" + context.getFireTime());
//        System.out.println("下次执行时间：" + context.getNextFireTime());

//        记录任务执行次数
        JobDataMap jobDataMap = jobDetail.getJobDataMap();
//        Integer count = (Integer) jobDataMap.get("count");
//        System.out.println("第" + count + "次执行");
//        jobDataMap.put("count", ++count);
    }
}
