package cn.bitlinks.ems.module.acquisition.quartz.job;

import cn.bitlinks.ems.framework.common.core.StandingbookAcquisitionDetailDTO;
import cn.bitlinks.ems.framework.common.util.opcda.ItemStatus;
import cn.bitlinks.ems.framework.common.util.opcda.OpcDaUtils;
import cn.bitlinks.ems.module.acquisition.api.quartz.dto.ServiceSettingsDTO;
import cn.bitlinks.ems.module.acquisition.mq.message.AcquisitionMessage;
import cn.bitlinks.ems.module.acquisition.mq.producer.AcquisitionMessageBufferManager;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

import static cn.bitlinks.ems.framework.common.enums.CommonConstants.SPRING_PROFILES_ACTIVE_DEV;
import static cn.bitlinks.ems.framework.common.enums.CommonConstants.SPRING_PROFILES_ACTIVE_LOCAL;
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
    @Value("${spring.profiles.active}")
    private String env;
    @Resource
    private RedisTemplate<String, String> redisTemplate;
    @Resource
    private AcquisitionMessageBufferManager bufferManager;

    /**
     * mock数据初始值 上限
     */
    final BigDecimal MOCK_INIT_MAX = new BigDecimal("600");
    /**
     * mock数据初始值 下限
     */
    final BigDecimal MOCK_INIT_MIN = new BigDecimal("100");
    /**
     * mock数据增量值 上限
     */
    final BigDecimal MOCK_INCREMENT_MAX = new BigDecimal("5");
    /**
     * mock数据增量值 下限
     */
    final BigDecimal MOCK_INCREMENT_MIN = new BigDecimal("0");
    final int DECIMAL_SCALE = 10;

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
            Boolean deviceStatus =
                    (Boolean) jobDataMap.get(ACQUISITION_JOB_DATA_MAP_KEY_STATUS);
            if (!Boolean.TRUE.equals(deviceStatus)) {
                log.info("设备[{}] 状态为false, 不进行数据采集!", standingbookId);
                return;
            }
            // 验证数据
            if (Objects.isNull(standingbookId) || CollUtil.isEmpty(details)) {
                log.error("数据采集任务[{}] 数据缺失: standingbookId={}, details={}", jobName, standingbookId, details);
                return;
            }
            // 采集数据
            if (CollUtil.isEmpty(details)) {
                log.info("设备[{}] 无可采集参数!", standingbookId);
                return;
            }
            // 过滤出 status = true && (dataSite 不为空 或 公式不为空) 的参数
            List<StandingbookAcquisitionDetailDTO> paramDetails =
                    details.stream().filter(detail -> Boolean.TRUE.equals(detail.getStatus()) && (StringUtils.isNotEmpty(detail.getDataSite()) || StringUtils.isNotEmpty(detail.getActualFormula()))).collect(Collectors.toList());
            if (CollUtil.isEmpty(paramDetails)) {
                log.info("设备[{}] 无可采集参数, 没有需要采集的参数!", standingbookId);
                return;
            }
            // 过滤出有io地址的需要采集的参数值
            // 筛选 dataSite 不为空的记录
            List<String> dataSites = paramDetails.stream()
                    .filter(detail -> StringUtils.isNotEmpty(detail.getDataSite()))
                    .map(StandingbookAcquisitionDetailDTO::getDataSite)
                    .collect(Collectors.toList());

            if (CollUtil.isEmpty(dataSites)) {
                log.info("设备[{}] 无可采集参数, 没有配置io的参数!", standingbookId);
                return;
            }

            // 采集有io的参数的真实数据
            Map<String, ItemStatus> itemStatusMap;
            if (env.equals(SPRING_PROFILES_ACTIVE_LOCAL) || env.equals(SPRING_PROFILES_ACTIVE_DEV)) {
                itemStatusMap = mockData(dataSites);
            } else {
                // 从redis中获取对应协议客户端的数据，
                itemStatusMap = OpcDaUtils.batchGetValue(serviceSettingsDTO.getIpAddress(),
                        serviceSettingsDTO.getUsername(),
                        serviceSettingsDTO.getPassword(), serviceSettingsDTO.getClsid(), dataSites);
            }

            if (CollUtil.isEmpty(itemStatusMap)) {
                log.info("设备[{}] 无可采集参数, 配置的io采集不到数据!", standingbookId);
                return;
            }

            // 构造消息对象
            AcquisitionMessage acquisitionMessage = new AcquisitionMessage();
            acquisitionMessage.setStandingbookId(standingbookId);
            acquisitionMessage.setDetails(paramDetails);
            acquisitionMessage.setJobTime(DateUtil.toLocalDateTime(context.getFireTime()));
            acquisitionMessage.setItemStatusMap(itemStatusMap);

            // 选择 topic（基于 jobName）
            String topicName = getTopicName(jobName);
            boolean success = bufferManager.enqueueMessageWithTimeout(topicName, acquisitionMessage);
            if (!success) {
                // 队列满，消息没能入队
                log.error("【AcquisitionMessageSender】消息入队失败，需要额外处理, topic:{}, 消息内容: {}", topicName, JSONUtil.toJsonStr(acquisitionMessage));
            }
            log.info("数据采集任务[{}] 发送MQ消息: topic={}, payload={}", jobName, topicName, JSONUtil.toJsonStr(acquisitionMessage));
            log.info("数据采集任务[{}] 完成", jobName);
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

    /**
     * 模拟采集的数据
     *
     * @param dataSites 数据点位集合
     */
    private Map<String, ItemStatus> mockData(List<String> dataSites) {
        Map<String, ItemStatus> resultMap = new HashMap<>();
        // mock 采集时间
        LocalDateTime collectTime = LocalDateTime.now();
        dataSites.forEach(dataSite -> {
            ItemStatus itemStatus = new ItemStatus();
            itemStatus.setItemId(dataSite);
            String redisKey = String.format(ACQUISITION_JOB_REDIS_KEY, env, dataSite);
            // 从 Redis 读取参数值
            String currentValue = redisTemplate.opsForValue().get(redisKey);
            //如果不存在最新数据, 随机生成 100-600之间的数值
            if (StringUtils.isEmpty(currentValue)) {
                currentValue = getRandomValue(MOCK_INIT_MAX, MOCK_INIT_MIN);
            } else {
                // 存在最新数据, 在最新数据的基础上进行随机0-5的数据的增加
                // 已存在：增加 0-5
                String increment = getRandomValue(MOCK_INCREMENT_MAX, MOCK_INCREMENT_MIN);
                currentValue = new BigDecimal(currentValue).add(new BigDecimal(increment)).toString();
            }
            // 存回 Redis
            redisTemplate.opsForValue().set(redisKey, currentValue);
            itemStatus.setValue(currentValue);
            itemStatus.setTime(collectTime);
            resultMap.put(dataSite, itemStatus);
        });
        return resultMap;

    }

    /**
     * 生成随机数据
     *
     * @param max 范围
     * @param min 范围
     * @return 随机数据
     */
    private String getRandomValue(BigDecimal max, BigDecimal min) {
        // 使用 Random 生成随机数
        Random random = new Random();
        // 生成 [0, 1) 的随机值，并缩放到 [min, max]
        BigDecimal range = max.subtract(min);
        BigDecimal randomValue =
                min.add(range.multiply(BigDecimal.valueOf(random.nextDouble()))).setScale(DECIMAL_SCALE,
                        RoundingMode.UP);
        return randomValue.toString();
    }
}
