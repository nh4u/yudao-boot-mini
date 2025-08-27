package cn.bitlinks.ems.module.power.task;


import cn.bitlinks.ems.framework.tenant.core.job.TenantJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 产量外部接口 定时任务
 *
 * @author liumingqiang
 */
@Slf4j
//@Component
public class ProductionTask {

    /**
     * 执行定时任务  同步产量数据到数据表中
     */
    @Scheduled(cron = "")
    @TenantJob
    public void execute() {

        // TODO: 2025/8/22  执行定时任务

    }


}