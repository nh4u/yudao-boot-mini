package cn.bitlinks.ems.module.acquisition.task;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class ThreadPoolConfig {

    /**
     * 配置OPC数据采集专用线程池
     */
    @Bean(name = "collectorAggExecutor")
    public ThreadPoolTaskExecutor collectorAggExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 核心线程数：根据CPU核心数和任务特性调整
        executor.setCorePoolSize(Runtime.getRuntime().availableProcessors() * 2);
        // 最大线程数
        executor.setMaxPoolSize(Runtime.getRuntime().availableProcessors() * 4);
        // 队列容量：用于缓冲等待执行的任务
        executor.setQueueCapacity(1000);
        // 线程空闲时间：超过核心线程数的线程，空闲多久后销毁
        executor.setKeepAliveSeconds(60);
        // 线程名称前缀：便于日志追踪
        executor.setThreadNamePrefix("collector-agg-");
        // 拒绝策略：当线程池和队列都满时，让提交任务的线程执行，起到限流作用
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 初始化线程池
        executor.initialize();
        return executor;
    }
}
    