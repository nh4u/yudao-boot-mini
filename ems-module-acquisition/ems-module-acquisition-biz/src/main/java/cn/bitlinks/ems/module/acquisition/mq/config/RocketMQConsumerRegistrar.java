package cn.bitlinks.ems.module.acquisition.mq.config;

import cn.bitlinks.ems.module.acquisition.mq.consumer.BaseConsumer;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.acl.common.AclClientRPCHook;
import org.apache.rocketmq.acl.common.SessionCredentials;
import org.apache.rocketmq.client.AccessChannel;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.remoting.RPCHook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

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

    @Resource
    private BaseConsumer baseConsumer;

    @PostConstruct
    public void init() throws MQClientException {
        RPCHook rpcHook = new AclClientRPCHook(new SessionCredentials(accessKey, secretKey));
        for (MultiConsumerProperties.ConsumerConfig config : properties.getConsumers()) {
            DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(config.getGroup(), rpcHook);
            consumer.setNamesrvAddr(mqServer);
            consumer.setAccessChannel(accessChannel);
            consumer.subscribe(config.getTopic(), "*");
            consumer.registerMessageListener(baseConsumer); // ✅ 使用你的 BaseConsumer
            consumer.start();
            log.info("✅ 启动 Consumer, topic={}, group={}", config.getTopic(), config.getGroup());
        }
    }
}
