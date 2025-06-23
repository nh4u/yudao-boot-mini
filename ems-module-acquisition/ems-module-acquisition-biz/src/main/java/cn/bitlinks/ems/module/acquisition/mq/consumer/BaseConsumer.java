
package cn.bitlinks.ems.module.acquisition.mq.consumer;

import cn.bitlinks.ems.framework.common.core.ParameterKey;
import cn.bitlinks.ems.framework.common.core.StandingbookAcquisitionDetailDTO;
import cn.bitlinks.ems.framework.common.util.calc.AcquisitionFormulaUtils;
import cn.bitlinks.ems.framework.common.util.opcda.ItemStatus;
import cn.bitlinks.ems.module.acquisition.dal.dataobject.collectrawdata.CollectRawDataDO;
import cn.bitlinks.ems.module.acquisition.mq.message.AcquisitionMessage;
import cn.bitlinks.ems.module.acquisition.starrocks.StarRocksStreamLoadService;
import cn.hutool.core.collection.CollUtil;
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
import java.time.ZoneId;
import java.util.*;

import static cn.bitlinks.ems.module.acquisition.enums.CommonConstants.STREAM_LOAD_PREFIX;


@Component
@Slf4j
public class BaseConsumer implements MessageListenerConcurrently {
    @Resource
    private StarRocksStreamLoadService starRocksStreamLoadService;
    private static final String TABLE_NAME = "collect_raw_data";

    @Override
    public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
        log.info("🔥 BaseConsumer 触发消息消费，共收到 {} 条", msgs.size());
        for (MessageExt msg : msgs) {
            String topic = msg.getTopic();
            String jsonStr = new String(msg.getBody(), StandardCharsets.UTF_8);
            log.info("✅ 消费到消息: topic={}, body={}", topic, jsonStr);
            AcquisitionMessage acquisitionMessage = JSONUtil.toBean(jsonStr, AcquisitionMessage.class);
            log.info("数据采集任务接收到mq消息：{}", JSONUtil.toJsonStr(acquisitionMessage));
            try {
                Map<String, ItemStatus> itemStatusMap = acquisitionMessage.getItemStatusMap();
                List<CollectRawDataDO> collectRawDataDOList = new ArrayList<>();
                // 将采集到的数据通过计算后，插入实时数据库
                Map<ParameterKey,
                        StandingbookAcquisitionDetailDTO> paramMap = new HashMap<>();
                for (StandingbookAcquisitionDetailDTO detail : acquisitionMessage.getDetails()) {
                    ParameterKey key = new ParameterKey(detail.getCode(), detail.getEnergyFlag());
                    paramMap.put(key, detail);
                }

                paramMap.forEach((key, detail) -> {
                    // 计算公式的值
                    String calcValue = AcquisitionFormulaUtils.calcSingleParamValue(detail, paramMap, itemStatusMap);
                    if (StringUtils.isEmpty(calcValue)) {
                        log.info("单个计算值为空，不进行数据插入,{}", JSONUtil.toJsonStr(detail));
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
                    collectRawDataDOList.add(collectRawDataDO);
                });

                if (CollUtil.isEmpty(collectRawDataDOList)) {
                    log.info("计算值后数据全为空，不进行数据插入,{}", JSONUtil.toJsonStr(acquisitionMessage.getStandingbookId()));
                    continue;
                }
                // 执行插入操作
                String labelName =
                        acquisitionMessage.getStandingbookId() + STREAM_LOAD_PREFIX + acquisitionMessage.getJobTime().atZone(ZoneId.systemDefault()).toEpochSecond();

                starRocksStreamLoadService.streamLoadData(collectRawDataDOList, labelName, TABLE_NAME);
            } catch (Exception e) {
                log.error("实时数据 台账id：{}，新增失败：", acquisitionMessage.getStandingbookId(), e);
            }
        }
        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
    }
}
