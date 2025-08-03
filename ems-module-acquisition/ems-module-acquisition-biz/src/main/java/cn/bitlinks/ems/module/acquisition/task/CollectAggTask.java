package cn.bitlinks.ems.module.acquisition.task;

import cn.bitlinks.ems.framework.common.core.StandingbookAcquisitionDetailDTO;
import cn.bitlinks.ems.framework.common.util.json.JsonUtils;
import cn.bitlinks.ems.framework.common.util.opcda.ItemStatus;
import cn.bitlinks.ems.framework.common.util.string.StrUtils;
import cn.bitlinks.ems.module.acquisition.mq.message.AcquisitionMessage;
import cn.bitlinks.ems.module.acquisition.mq.producer.AcquisitionMessageBufferManager;
import cn.bitlinks.ems.module.acquisition.service.collectrawdata.ServerDataService;
import cn.bitlinks.ems.module.power.dto.DeviceCollectCacheDTO;
import cn.bitlinks.ems.module.power.dto.ServerStandingbookCacheDTO;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static cn.bitlinks.ems.module.acquisition.enums.CommonConstants.*;
import static cn.bitlinks.ems.module.power.enums.RedisKeyConstants.STANDING_BOOK_ACQ_CONFIG_PREFIX;
import static cn.bitlinks.ems.module.power.enums.RedisKeyConstants.STANDING_BOOK_SERVER_DEVICE_CONFIG;

/**
 * 全部采集数据
 */
@Slf4j
@Component
public class CollectAggTask {


    @Resource
    private ServerDataService serverDataService;
    @Resource
    private AcquisitionMessageBufferManager bufferManager;

    @Value("${rocketmq.topic.device-acquisition}")
    private String deviceTaskTopic;
    @Resource
    private RedisTemplate<String, byte[]> byteArrayRedisTemplate;
    @Resource
    private RedisTemplate<String, String> redisTemplate;
    @Value("${spring.profiles.active}")
    private String env;
    @Resource
    private RedissonClient redissonClient;
    private static final ForkJoinPool forkJoinPool = new ForkJoinPool(8); // 线程数可配置

    // 每秒执行一次
    @Scheduled(fixedRate = 1000) // fixedRate表示以上一次任务开始时间为基准，间隔1秒
    public void collectData() {

        String LOCK_KEY = String.format(COLLECT_AGG_TASK_LOCK_KEY, env);

        RLock lock = redissonClient.getLock(LOCK_KEY);
        try {
            if (!lock.tryLock(5000L, TimeUnit.MILLISECONDS)) {
                log.info("实时数据入库redis Task 已由其他节点执行");
            }
            try {
                log.info("实时数据入库redis Task 开始执行");
                serverDataService.processServerData();
                log.info("实时数据入库redis Task 执行完成");
            } finally {
                lock.unlock();
            }
        } catch (Exception e) {
            log.error("实时数据入库redis 失败", e);
        }
    }


    //每秒扫描全部设备匹配时间
    @Scheduled(fixedRate = 1000)
    public void scheduledScanAllDevices() {

        String LOCK_KEY = String.format(COLLECT_AGG_TASK_SCAN_LOCK_KEY, env);

        RLock lock = redissonClient.getLock(LOCK_KEY);
        try {
            if (!lock.tryLock(5000L, TimeUnit.MILLISECONDS)) {
                log.info("实时数据扫描台账Task 已由其他节点执行");
            }
            try {
                log.info("实时数据扫描台账Task 开始执行");
                scanAllDevices();
                log.info("实时数据扫描台账Task 执行完成");
            } finally {
                lock.unlock();
            }
        } catch (Exception e) {
            log.error("实时数据扫描台账失败", e);
        }
    }

    private void processSingleDevice(Long deviceId, Map<String, String> entryMap, LocalDateTime jobTime) {
        // 查询redis中设备id对应的数采配置
        String sbConfigKey = String.format(STANDING_BOOK_ACQ_CONFIG_PREFIX, deviceId);
        String deviceAcqConfigStr = redisTemplate.opsForValue().get(sbConfigKey);
        if (Objects.isNull(deviceAcqConfigStr)) {
            return;
        }
        DeviceCollectCacheDTO deviceCollectCacheDTO = JSONObject.parseObject(deviceAcqConfigStr, DeviceCollectCacheDTO.class); // 转回对象
        if (Objects.isNull(deviceCollectCacheDTO)) {
            return;
        }
        // 首先匹配更新频率是否符合
        Long intervalSeconds = deviceCollectCacheDTO.getFrequency();
        long diffInSeconds = Duration.between(deviceCollectCacheDTO.getJobStartTime(), jobTime).getSeconds();
        // 更新频率不满足，不应该采集
        if (diffInSeconds < 0 || diffInSeconds % intervalSeconds != 0) {
            return;
        }
        // 采集相应数据
        Set<String> dataSitesSet = new HashSet<>(deviceCollectCacheDTO.getDataSites());
        // 构造消息对象

        // 筛选匹配 dataSites 的数据
        Map<String, ItemStatus> filteredMap = entryMap.entrySet().stream()
                .filter(entry -> dataSitesSet.contains(entry.getKey()))
                .map(entry -> {
                    ItemStatus itemStatus = JSONObject.parseObject(entry.getValue(), ItemStatus.class);
                    if (itemStatus != null) {
                        Map<String, ItemStatus> singleEntry = new HashMap<>();
                        singleEntry.put(entry.getKey(), itemStatus);
                        return singleEntry;
                    } else {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .flatMap(map -> map.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        // 构造消息对象
        AcquisitionMessage acquisitionMessage = new AcquisitionMessage();
        acquisitionMessage.setStandingbookId(deviceId);
        List<StandingbookAcquisitionDetailDTO> acquisitionDetailDTOS = deviceCollectCacheDTO.getDetails();
        acquisitionMessage.setDetails(acquisitionDetailDTOS);
        acquisitionMessage.setJobTime(jobTime);
        acquisitionMessage.setItemStatusMap(filteredMap);

        // 选择 topic（基于 jobName）
        String topicName = getTopicName(String.format(ACQUISITION_JOB_NAME_PREFIX, deviceId));
        boolean success = bufferManager.enqueueMessageWithTimeout(topicName, acquisitionMessage);
        if (!success) {
            // 队列满，消息没能入队
            log.error("【AcquisitionMessageSender】消息入队失败，需要额外处理, topic:{}, 消息内容: {}", topicName, JSONUtil.toJsonStr(acquisitionMessage));
        }
        log.info("数据采集任务[{}] 发送MQ消息: topic={}, payload={}", deviceId, topicName, JSONUtil.toJsonStr(acquisitionMessage));
        log.info("数据采集任务[{}] 完成", deviceId);
    }

    private void scanAllDevices() {

        // 当成30s之前的任务时间，反正redis缓存了五分钟的数据。先试试水
        LocalDateTime jobTime = LocalDateTime.now().minusSeconds(30);
        String timestampStr = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(jobTime);

        // 获取服务-》台账ids的映射
        Map<String, List<Long>> serverDevicesMapping = getServerDevicesMapping();
        if (CollUtil.isEmpty(serverDevicesMapping)) {
            return;
        }
        serverDevicesMapping.forEach((serverKey, deviceIds) -> {
            // 查询 服务key在当前时间的采集数据
            String hashKey = String.format(COLLECTOR_AGG_REALTIME_CACHE_KEY, serverKey, timestampStr);
            Map<Object, Object> entries = redisTemplate.opsForHash().entries(hashKey);
            Map<String, String> entryMap = entries.entrySet().stream()
                    .collect(Collectors.toMap(e -> String.valueOf(e.getKey()), e -> String.valueOf(e.getValue())));
            if (CollUtil.isEmpty(deviceIds)) return;
            // 遍历设备id,匹配所有的设备id去获取自己的数据，
            deviceIds.forEach(deviceId -> processSingleDevice(deviceId, entryMap, jobTime));
            // 分批并发处理，每批最多200个设备
            int batchSize = 200;
            List<List<Long>> batches = CollUtil.split(deviceIds, batchSize);

            forkJoinPool.submit(() -> batches.parallelStream().forEach(batch -> {
                batch.forEach(deviceId -> processSingleDevice(deviceId, entryMap, jobTime));
            })).join(); // 等待所有子任务完成
        });

    }

    public String getTopicName(String jobName) {
        int groupIndex = Math.abs(jobName.hashCode() % 3);
        return deviceTaskTopic + groupIndex;
    }

    /**
     * 从redis中获取数采服务器与设备id映射关系
     *
     * @return
     */
    private Map<String, List<Long>> getServerDevicesMapping() {
        byte[] compressed = byteArrayRedisTemplate.opsForValue().get(STANDING_BOOK_SERVER_DEVICE_CONFIG);

        // 先判断缓存是否存在，避免空指针
        if (compressed == null) {
            log.info("缓存不存在，返回空映射");
            return new HashMap<>(); // 或返回null，根据业务需求处理
        }

        try {
            String cacheRes = StrUtils.decompressGzip(compressed);
            if (CharSequenceUtil.isNotEmpty(cacheRes)) {
                // 用 Jackson 处理泛型转换
                // 用 Jackson 处理泛型转换
                List<ServerStandingbookCacheDTO> serverStandingbookList = JsonUtils.parseArray(cacheRes, ServerStandingbookCacheDTO.class);
                return serverStandingbookList.stream()
                        .collect(Collectors.groupingBy(
                                ServerStandingbookCacheDTO::getServerKey,
                                Collectors.mapping(ServerStandingbookCacheDTO::getStandingbookId, Collectors.toList())
                        ));
            }
        } catch (Exception e) {
            log.error("解析缓存的服务器设备id映射关系失败", e);
        }

        return new HashMap<>(); // 解析失败时返回空映射
    }
}
