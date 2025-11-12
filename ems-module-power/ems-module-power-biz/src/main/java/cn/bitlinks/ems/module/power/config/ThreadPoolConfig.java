package cn.bitlinks.ems.module.power.config;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.*;

@Configuration
public class ThreadPoolConfig {
    private static final int THREAD_POOL_SIZE = 4;

    @Bean(name = "splitTaskExecutor")
    public Executor splitTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4); // 根据机器性能调整
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("split-task-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(10);
        scheduler.setThreadNamePrefix("scheduled-task-");
        scheduler.initialize();
        return scheduler;
    }
    /**
     * StarRocks异步导入专用线程池：全局单例，用于处理StarRocks流式导入的异步任务
     */
    @Bean(name = "starRocksAsyncExecutor") // 给线程池起名字，方便后续按名称注入
    public ExecutorService starRocksAsyncExecutor() {

        return new ThreadPoolExecutor(
                THREAD_POOL_SIZE,                      // 核心线程数：根据StarRocks集群承载能力调整（如16核CPU设16）
                8,                      // 最大线程数：核心线程忙不过来时，最多再创建16个临时线程
                30,                      // 空闲线程存活时间：临时线程空闲60秒后销毁，避免资源占用
                TimeUnit.SECONDS,        // 时间单位：秒
                new LinkedBlockingQueue<>(1000), // 任务队列：最多缓存2000个待执行任务
                new ThreadFactoryBuilder().setNameFormat("starrocks-async-%d").build(),
                new ThreadPoolExecutor.DiscardOldestPolicy() // 队列满时策略：丢弃最旧的任务（可根据业务改）
        );
    }
}
    