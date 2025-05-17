package cn.bitlinks.ems.module.acquisition.quartz.job;

import cn.bitlinks.ems.framework.common.core.StandingbookAcquisitionDetailDTO;
import cn.bitlinks.ems.module.acquisition.api.job.dto.ServiceSettingsDTO;
import cn.bitlinks.ems.module.acquisition.mq.message.AcquisitionMessage;
import cn.hutool.core.date.DateUtil;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;

import static cn.bitlinks.ems.module.acquisition.enums.CommonConstants.*;

/**
 * 数据采集定时任务
 */
@Slf4j
@Component
@PersistJobDataAfterExecution//让执行次数会递增
@DisallowConcurrentExecution//禁止并发执行
public class AcquisitionJob implements Job {

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    @Value("${rocketmq.topic.device-acquisition}")
    private String deviceTaskTopic;

    public void execute(JobExecutionContext context) {

        String jobName = context.getJobDetail().getKey().getName();
        JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();
        try {
            log.info("数据采集任务[{}] 执行时间:{}", jobName, context.getFireTime());
            Long standingbookId =
                    (Long) jobDataMap.get(ACQUISITION_JOB_DATA_MAP_KEY_STANDING_BOOK_ID);
            List<StandingbookAcquisitionDetailDTO> details = (List<StandingbookAcquisitionDetailDTO>) jobDataMap.get(ACQUISITION_JOB_DATA_MAP_KEY_DETAILS);
            ServiceSettingsDTO serviceSettingsDTO =
                    (ServiceSettingsDTO) jobDataMap.get(ACQUISITION_JOB_DATA_MAP_KEY_SERVICE_SETTINGS);
            // 验证数据
            if (Objects.isNull(standingbookId) || Objects.isNull(details)) {
                log.error("数据采集任务[{}] 数据缺失: standingbookId={}, details={}", jobName, standingbookId, details);
                return;
            }

            // 构造消息对象
            AcquisitionMessage acquisitionMessage = new AcquisitionMessage();
            acquisitionMessage.setStandingbookId(standingbookId);
            acquisitionMessage.setDetails(details);
            acquisitionMessage.setServiceSettingsDTO(serviceSettingsDTO);
            acquisitionMessage.setJobTime(DateUtil.toLocalDateTime(context.getFireTime()));

            // 构建 RocketMQ 消息
            Message<AcquisitionMessage> msg = MessageBuilder.withPayload(acquisitionMessage).build();

            // 选择 topic（基于 jobName）
            String topicName = getTopicName(jobName);

            // 发送消息
            rocketMQTemplate.send(topicName, msg);
            log.info("数据采集任务[{}] 发送MQ消息: topic={}, payload={}", jobName, topicName, JSONUtil.toJsonStr(acquisitionMessage));
        } catch (Exception e) {
            log.error("数据采集任务[{}] 发送 MQ 消息失败", jobName, e);
        }

    }

    /**
     * 根据任务名称分组, 使用哈希取模分配到三组
     *
     * @param jobName 任务名称
     * @return MQ topic名称
     */
    public String getTopicName(String jobName) {
        int groupIndex = Math.abs(jobName.hashCode() % 3);
        return deviceTaskTopic + groupIndex;
    }
}
