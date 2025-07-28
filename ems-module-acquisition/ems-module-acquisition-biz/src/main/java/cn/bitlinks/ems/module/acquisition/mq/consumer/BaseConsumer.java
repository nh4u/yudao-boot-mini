
package cn.bitlinks.ems.module.acquisition.mq.consumer;

import cn.bitlinks.ems.framework.common.core.ParameterKey;
import cn.bitlinks.ems.framework.common.core.StandingbookAcquisitionDetailDTO;
import cn.bitlinks.ems.framework.common.util.calc.AcquisitionFormulaUtils;
import cn.bitlinks.ems.framework.common.util.opcda.ItemStatus;
import cn.bitlinks.ems.module.acquisition.dal.dataobject.collectrawdata.CollectRawDataDO;
import cn.bitlinks.ems.module.acquisition.mq.message.AcquisitionMessage;
import cn.bitlinks.ems.module.acquisition.starrocks.StreamLoadBufferWorker;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


@Component
@Slf4j
public class BaseConsumer implements MessageListenerConcurrently {
    @Resource
    private StreamLoadBufferWorker streamLoadBufferWorker;

    @Override
    public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
        log.info("🔥 BaseConsumer 触发消息消费，共收到 {} 条", msgs.size());
        long start = System.currentTimeMillis();
        for (MessageExt msg : msgs) {

            String jsonStr = new String(msg.getBody(), StandardCharsets.UTF_8);

            List<AcquisitionMessage> acquisitionMessages = JSONUtil.toList(jsonStr, AcquisitionMessage.class);
            log.info("数据采集任务接收到mq消息：{}", JSONUtil.toJsonStr(acquisitionMessages));
            for (AcquisitionMessage acquisitionMessage : acquisitionMessages) {
                // 你原来的处理逻辑：计算 → 封装 → 调用 StarRocksStreamLoadService
                try {
                    Map<String, ItemStatus> itemStatusMap = acquisitionMessage.getItemStatusMap();
                    Map<ParameterKey, StandingbookAcquisitionDetailDTO> paramMap = new HashMap<>();
                    for (StandingbookAcquisitionDetailDTO detail : acquisitionMessage.getDetails()) {
                        paramMap.put(new ParameterKey(detail.getCode(), detail.getEnergyFlag()), detail);
                    }

                    paramMap.forEach((key, detail) -> {
                        // 计算公式的值
                        String calcValue = AcquisitionFormulaUtils.calcSingleParamValue(detail, paramMap, itemStatusMap);
                        if (StringUtils.isEmpty(calcValue)) {
                            log.info("【BaseConsumer】单个计算值为空，不进行数据插入,{}", JSONUtil.toJsonStr(detail));
                            return;
                        }
                        // 计算出值, 将数据带入实时数据表中.
                        CollectRawDataDO collectRawDataDO = new CollectRawDataDO();
                        collectRawDataDO.setDataSite(detail.getDataSite());
                        collectRawDataDO.setStandingbookId(acquisitionMessage.getStandingbookId());
                        collectRawDataDO.setSyncTime(acquisitionMessage.getJobTime());
                        collectRawDataDO.setParamCode(detail.getCode());

                        collectRawDataDO.setUsage(detail.getUsage());
                        collectRawDataDO.setEnergyFlag(detail.getEnergyFlag());
                        collectRawDataDO.setCalcValue(calcValue);
                        ItemStatus itemStatus = itemStatusMap.get(detail.getDataSite());
                        if (Objects.nonNull(itemStatus)) {
                            collectRawDataDO.setRawValue(itemStatus.getValue());
                            collectRawDataDO.setCollectTime(itemStatus.getTime());
                        }
                        collectRawDataDO.setCreateTime(LocalDateTime.now());
                        collectRawDataDO.setFullIncrement(detail.getFullIncrement());
                        collectRawDataDO.setDataType(detail.getDataType());
                        collectRawDataDO.setDataFeature(detail.getDataFeature());
                        streamLoadBufferWorker.offer(collectRawDataDO); // 投递到队列中
                    });

                } catch (Exception e) {
                    log.error("【BaseConsumer】实时数据 台账id：{}，消费计算异常", acquisitionMessage.getStandingbookId(), e);
                }
            }
        }

        log.info("🔥 BaseConsumer 完成批次采集处理：{} 条，用时 {} ms", msgs.size(), System.currentTimeMillis() - start);

        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
    }

}
