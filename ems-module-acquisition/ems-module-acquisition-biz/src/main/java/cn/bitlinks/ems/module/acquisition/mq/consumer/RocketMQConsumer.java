package cn.bitlinks.ems.module.acquisition.mq.consumer;

import cn.bitlinks.ems.module.acquisition.mq.message.AcquisitionMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQListener;

@Slf4j
public abstract class RocketMQConsumer implements RocketMQListener<AcquisitionMessage> {

    @Override
    public void onMessage(AcquisitionMessage acquisitionMessage) {

        log.info("收到消息：{}", acquisitionMessage);
        // TODO: 处理消息


//        /**
//         * 台账id
//         */
//        private String standingbookId;
//        /**
//         * 数采参数列表
//         */
//        private List<StandingbookAcquisitionDetailDTO> details;

    }
}
