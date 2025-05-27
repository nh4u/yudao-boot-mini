package cn.bitlinks.ems.module.power.mq.consumer;

import org.apache.rocketmq.remoting.protocol.heartbeat.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Service;

import java.util.List;

import javax.annotation.Resource;

import cn.bitlinks.ems.module.acquisition.api.collectrawdata.dto.MinuteAggregateDataDTO;
import cn.bitlinks.ems.module.power.service.usagecost.CalcUsageCostService;
import cn.hutool.core.collection.CollectionUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * @author wangl
 * @date 2025年05月26日 13:47
 */
@Slf4j
@Service
@RocketMQMessageListener(
        topic = "${rocketmq.device-aggregate-consumer.device-aggregate}",
        consumerGroup = "${rocketmq.device-aggregate-consumer.group}"
)
public class AggregateConsumer  implements RocketMQListener<List<MinuteAggregateDataDTO>> {

    @Resource
    private CalcUsageCostService calcUsageCostService;

    @Override
    public void onMessage(List<MinuteAggregateDataDTO> messages) {
        if(CollectionUtil.isEmpty(messages)){
            log.info("AggregateConsumer get no message");
            return;
        }
        calcUsageCostService.process(messages);
        log.info("AggregateConsumer end");
    }
}
