package cn.bitlinks.ems.module.acquisition.mq.config;

import org.apache.rocketmq.client.AccessChannel;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.remoting.RPCHook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@EnableConfigurationProperties(MultiConsumerProperties.class)
public class RocketMQConsumerRegistrar {

    @Autowired
    private MultiConsumerProperties properties;

    @Value("${rocketmq.name-server}")
    private String mqServer;
    @Value("${rocketmq.consumer.access-key}")
    private String accessKey;
    @Value("${rocketmq.consumer.secret-key}")
    private String secretKey;
    private final AccessChannel accessChannel = AccessChannel.LOCAL;

    @PostConstruct
    public void init() throws MQClientException {

        RPCHook rpcHook = new CustomAuthRPCHook(accessKey, secretKey);

        for (MultiConsumerProperties.ConsumerConfig config : properties.getConsumers()) {
            DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(config.getGroup(), rpcHook);
            consumer.setNamesrvAddr(mqServer);
            consumer.setAccessChannel(accessChannel);
            consumer.subscribe(config.getTopic(), "*");
            consumer.registerMessageListener((MessageListenerConcurrently) (msgs, context) -> {
                log.info("消费组 [" + config.getGroup() + "] 收到消息: " + new String(msgs.get(0).getBody()));
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            });
            consumer.start();
        }
    }
}
