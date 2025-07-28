package cn.bitlinks.ems.module.acquisition.mq.producer;

import cn.bitlinks.ems.module.acquisition.mq.message.AcquisitionMessage;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Component
public class AcquisitionMessageBufferManager {

    private static final int QUEUE_CAPACITY = 20000;
    private static final int BATCH_SIZE = 64;
    private static final int SEND_INTERVAL_SECONDS = 2;

    private final BlockingQueue<BufferedMessage> messageQueue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    @PostConstruct
    public void startBatchSender() {
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(this::sendBatch, 0, SEND_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    public boolean enqueueMessageWithTimeout(String topic, AcquisitionMessage acquisitionMessage) {
        BufferedMessage bufferedMessage = new BufferedMessage(topic, acquisitionMessage);
        try {
            boolean offered = messageQueue.offer(bufferedMessage, 3, TimeUnit.SECONDS);
            if (!offered) {
                log.warn("【AcquisitionMessageBufferManager】队列满，超时3秒未入队，丢弃消息");
            }
            return offered;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("【AcquisitionMessageBufferManager】入队超时被中断，消息丢失", e);
            return false;
        }
    }

    /**
     * 批量发送逻辑
     */
    private void sendBatch() {
        List<BufferedMessage> batch = new ArrayList<>();
        // 最多取出 64条
        messageQueue.drainTo(batch, BATCH_SIZE);

        if (batch.isEmpty()) {
            return;
        }

        // 按 topic 分组批量发送
        Map<String, List<AcquisitionMessage>> topicMap = batch.stream()
                .collect(Collectors.groupingBy(
                        BufferedMessage::getTopic,
                        Collectors.mapping(BufferedMessage::getMessage, Collectors.toList())
                ));
        for (Map.Entry<String, List<AcquisitionMessage>> entry : topicMap.entrySet()) {
            String topic = entry.getKey();
            List<AcquisitionMessage> messages = entry.getValue();

            // 构建一个新的批量消息体（payload为List）
            Message<List<AcquisitionMessage>> batchMsg = MessageBuilder.withPayload(messages).build();

            try {
                rocketMQTemplate.send(topic, batchMsg);
                log.info("【AcquisitionMessageBufferManager】批量发送 {} 条 MQ 消息到 topic={}", messages.size(), topic);
            } catch (Exception e) {
                log.error("【AcquisitionMessageBufferManager】批量发送消息失败: topic={}, payload={}", topic, JSONUtil.toJsonStr(messages), e);
            }
        }

    }


}
