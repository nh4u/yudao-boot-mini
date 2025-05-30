package cn.bitlinks.ems.module.acquisition.mq.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "rocketmq.multi-consumer")
public class MultiConsumerProperties {
    private List<ConsumerConfig> consumers;

    @Data
    public static class ConsumerConfig {
        private String group;
        private String topic;
    }
}
