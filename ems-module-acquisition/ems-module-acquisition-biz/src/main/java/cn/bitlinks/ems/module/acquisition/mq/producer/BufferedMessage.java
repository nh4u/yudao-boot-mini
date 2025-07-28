package cn.bitlinks.ems.module.acquisition.mq.producer;

import cn.bitlinks.ems.module.acquisition.mq.message.AcquisitionMessage;


import lombok.Data;
import lombok.AllArgsConstructor;

@Data
@AllArgsConstructor
public class BufferedMessage {
    private final String topic;
    private final AcquisitionMessage message;
}