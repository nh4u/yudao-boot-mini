package cn.bitlinks.ems.module.acquisition.mq.config;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import cn.bitlinks.ems.module.acquisition.mq.consumer.BaseConsumer;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class DynamicConsumerManager {
    @Value("${rocketmq.name-server}")
    private String mqServer;
    private final MultiConsumerProperties multiConsumerProperties;

    public DynamicConsumerManager(MultiConsumerProperties multiConsumerProperties) {
        this.multiConsumerProperties = multiConsumerProperties;
    }

    @PostConstruct
    public void startConsumers() throws MQClientException {
        MessageListenerConcurrently listener = new BaseConsumer();

        for (MultiConsumerProperties.ConsumerConfig config : multiConsumerProperties.getConsumers()) {
            DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(config.getGroup());
            consumer.setNamesrvAddr(mqServer);
            consumer.subscribe(config.getTopic(), "*");
            consumer.registerMessageListener(listener);
            consumer.start();

            log.info("✅ 已启动消费者: group={}, topic={}", config.getGroup(), config.getTopic());
        }
    }
}
