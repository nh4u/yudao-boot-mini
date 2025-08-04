package cn.bitlinks.ems.module.acquisition.starrocks;

import cn.bitlinks.ems.module.acquisition.dal.dataobject.collectrawdata.CollectRawDataDO;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static cn.bitlinks.ems.module.acquisition.enums.CommonConstants.STREAM_LOAD_BUFFER_PREFIX;

@Component
@Slf4j
public class StreamLoadBufferWorker {

    @Resource
    private StarRocksStreamLoadService starRocksStreamLoadService;
    private static final int QUEUE_CAPACITY = 50000;

    private static final String TABLE_NAME = "collect_raw_data";
    private static final int BATCH_SIZE = 2000;
    private static final long FLUSH_INTERVAL_MS = 5000;

    private final BlockingQueue<CollectRawDataDO> queue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
    private final List<CollectRawDataDO> buffer = new ArrayList<>(BATCH_SIZE);

    public StreamLoadBufferWorker() {
        startWorker();
    }

    public void offer(CollectRawDataDO data) {
        if (!queue.offer(data)) {
            log.warn("⚠️ StreamLoad 队列已满，数据被丢弃！data:{}",data);
        }
    }

    private void startWorker() {
        // 创建一个线程，用于处理队列中的数据
        Thread worker = new Thread(() -> {
            // 记录上次刷新的时间
            long lastFlushTime = System.currentTimeMillis();
            while (true) {
                try {
                    // 从队列中取出数据
                    CollectRawDataDO data = queue.poll(1, TimeUnit.SECONDS);
                    if (data != null) {
                        // 将数据添加到缓冲区
                        buffer.add(data);
                    }

                    // 获取当前时间
                    long now = System.currentTimeMillis();
                    // 如果缓冲区中的数据量达到阈值或者距离上次刷新的时间超过了刷新间隔，则进行刷新
                    if (buffer.size() >= BATCH_SIZE || (now - lastFlushTime) >= FLUSH_INTERVAL_MS) {
                        // 如果缓冲区不为空，则进行刷新
                        if (!buffer.isEmpty()) {
                            flush();
                            // 更新上次刷新的时间
                            lastFlushTime = now;
                        }
                    }

                } catch (Exception e) {
                    // 记录异常信息
                    log.error("❌ StreamLoadBufferWorker 出现异常", e);
                }
            }
        });
        // 设置线程为守护线程
        worker.setDaemon(true);
        // 设置线程名称
        worker.setName("stream-load-worker");
        // 启动线程
        worker.start();
    }

    private void flush() {
        // 生成一个唯一的标签名
        String labelName = System.currentTimeMillis() + STREAM_LOAD_BUFFER_PREFIX + RandomUtil.randomNumbers(6);
        try {
            // 记录日志，显示批量插入的数据量
            log.info("⬆️ 【实时数据】批量插入 StarRocks：{} 条", buffer.size());
            // 调用 StarRocksStreamLoadService 的 streamLoadData 方法，将数据插入 StarRocks
            starRocksStreamLoadService.streamLoadData(new ArrayList<>(buffer), labelName, TABLE_NAME);
        } catch (Exception e) {
            // 记录错误日志，显示插入失败的原因
            log.error("❌ 【实时数据】批量插入 StarRocks，label[{}] 失败：{}",labelName, e.getMessage(), e);
            log.error("❌ 【实时数据】批量插入 StarRocks，label[{}] 失败明细：{}", labelName,JSONUtil.toJsonStr(buffer));
        } finally {
            // 清空 buffer
            buffer.clear();
        }
    }
}
