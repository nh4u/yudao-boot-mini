package cn.bitlinks.ems.module.power.mq.consumer;

import cn.bitlinks.ems.module.acquisition.api.collectrawdata.dto.MinuteAggregateDataDTO;
import cn.bitlinks.ems.module.acquisition.api.collectrawdata.dto.MultiMinuteAggDataDTO;
import cn.bitlinks.ems.module.power.service.copsettings.CopCalcService;
import cn.bitlinks.ems.module.power.service.usagecost.CalcUsageCostService;
import cn.hutool.core.collection.CollectionUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @author wangl
 * @date 2025年05月26日 13:47
 */
@Slf4j
@Service
@RocketMQMessageListener(
        topic = "${rocketmq.topic.device-aggregate}",
        consumerGroup = "${rocketmq.consumer.group}"
)
public class AggregateConsumer implements RocketMQListener<MultiMinuteAggDataDTO> {

    @Resource
    private CalcUsageCostService calcUsageCostService;
    @Resource
    private CopCalcService copCalcService;

    @Override
    public void onMessage(MultiMinuteAggDataDTO messages) {
        List<MinuteAggregateDataDTO> minuteAggMsg= messages.getMinuteAggregateDataDTOList();
        if (CollectionUtil.isEmpty(messages.getMinuteAggregateDataDTOList())) {
            log.info("AggregateConsumer get no message");
            return;
        }
        calcUsageCostService.process(minuteAggMsg);
        // cop 重算逻辑，每一批次必定是全小时级别数据，不会出现跨小时的情况，
        // 1. 获取最小小时 和 最大小时
        if(messages.getCopFlag()) {
            LocalDateTime minHour = minuteAggMsg.stream()
                    .map(dto -> dto.getAggregateTime().withMinute(0).withSecond(0).withNano(0))
                    .min(LocalDateTime::compareTo).orElse(null);
            LocalDateTime maxHour = minuteAggMsg.stream()
                    .map(dto -> dto.getAggregateTime().withMinute(0).withSecond(0).withNano(0))
                    .max(LocalDateTime::compareTo).orElse(null);
            if (minHour == null || maxHour == null) {
                log.warn("cop计算逻辑 接收到空时间区间，跳过处理");
                return;
            }
            // 计算、重算cop逻辑
            copCalcService.calculateCop(minHour, maxHour.plusHours(1L), minuteAggMsg);
        }
        log.info("AggregateConsumer end");
    }
}
