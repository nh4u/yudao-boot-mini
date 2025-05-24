package cn.bitlinks.ems.module.acquisition.mq.consumer;

import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.springframework.stereotype.Component;

@Component
@RocketMQMessageListener(topic = "${rocketmq.topic.device-acquisition}1", consumerGroup = "${rocketmq.producer.group}")
public class RocketMQConsumer1 extends RocketMQConsumer {


}
