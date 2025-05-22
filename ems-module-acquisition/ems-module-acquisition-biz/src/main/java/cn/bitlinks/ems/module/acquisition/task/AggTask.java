package cn.bitlinks.ems.module.acquisition.task;


import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AggTask {

    @Scheduled(fixedRate = 5000) // 每分钟执行一次
    public void doSomething() {
        // 查询所有的设备数采设置, 进行聚合 todo
        //
        //System.out.println("SimpleTask: " + System.currentTimeMillis());
    }
}