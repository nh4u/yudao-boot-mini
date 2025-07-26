package cn.bitlinks.ems.module.acquisition.service.minuteaggregatedata;

import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.framework.tenant.core.aop.TenantIgnore;
import cn.bitlinks.ems.module.acquisition.api.collectrawdata.dto.MinuteAggDataSplitDTO;
import cn.bitlinks.ems.module.acquisition.api.collectrawdata.dto.MinuteAggregateDataDTO;
import cn.bitlinks.ems.module.acquisition.api.collectrawdata.dto.MultiMinuteAggDataDTO;
import cn.bitlinks.ems.module.acquisition.api.minuteaggregatedata.dto.MinuteRangeDataParamDTO;
import cn.bitlinks.ems.module.acquisition.dal.dataobject.minuteaggregatedata.MinuteAggregateDataDO;
import cn.bitlinks.ems.module.acquisition.dal.mysql.minuteaggregatedata.MinuteAggregateDataMapper;
import cn.bitlinks.ems.module.acquisition.service.partition.PartitionService;
import cn.bitlinks.ems.module.acquisition.starrocks.StarRocksStreamLoadService;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.bitlinks.ems.module.acquisition.enums.CommonConstants.*;
import static cn.bitlinks.ems.module.acquisition.enums.ErrorCodeConstants.STREAM_LOAD_RANGE_FAIL;

/**
 * 分钟聚合数据service
 */
@DS("starrocks")
@Service
@Validated
@Slf4j
public class MinuteAggregateDataServiceImpl implements MinuteAggregateDataService {

    @Resource
    private MinuteAggregateDataMapper minuteAggregateDataMapper;

    @Resource
    private StarRocksStreamLoadService starRocksStreamLoadService;


    @Resource
    private RocketMQTemplate rocketMQTemplate;
    @Value("${rocketmq.topic.device-aggregate}")
    private String deviceAggTopic;
    private static final int THREAD_POOL_SIZE = 4;
    private static final RateLimiter MQ_RATE_LIMITER = RateLimiter.create(200); // 每秒200条消息
    // MQ异步发送队列
    private final BlockingQueue<MultiMinuteAggDataDTO> mqQueue = new LinkedBlockingQueue<>(100000);

    @Resource
    private PartitionService partitionService;

    // 启动MQ推送线程（建议在 @PostConstruct 中调用一次）
    @PostConstruct
    public void init() {
        startMqSenderThread();
    }

    public void startMqSenderThread() {
        new Thread(() -> {
            while (true) {
                try {
                    MultiMinuteAggDataDTO dto = mqQueue.take(); // 阻塞获取
                    MQ_RATE_LIMITER.acquire(); // 限速
                    Message<MultiMinuteAggDataDTO> msg = MessageBuilder.withPayload(dto).build();
                    rocketMQTemplate.send(deviceAggTopic, msg);
                    log.info("MQ消息已发送，header: {}", msg.getHeaders());
                } catch (Exception e) {
                    log.error("MQ发送失败", e);
                }
            }
        }, "mq-sender-thread").start();
    }


    @TenantIgnore
    public void sendMsgToUsageCostBatchNew(List<MinuteAggregateDataDO> aggDataList, Boolean copFlag) throws IOException {
        if (CollUtil.isEmpty(aggDataList)) {
            return;
        }

        // 1. 按小时分组，再按100条拆分小批
        List<List<MinuteAggregateDataDO>> batchList = getHourGroupBatch(aggDataList);

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        List<Future<Void>> futures = new ArrayList<>();

        for (List<MinuteAggregateDataDO> batch : batchList) {
            futures.add(executor.submit(() -> {
                try {
                    // 2. 写入 StarRocks
                    String labelName = System.currentTimeMillis() + STREAM_LOAD_PREFIX + RandomUtil.randomNumbers(6);
                    starRocksStreamLoadService.streamLoadData(batch, labelName, MINUTE_AGGREGATE_DATA_TB_NAME);

                    // 3. 构造 MQ DTO 并加入队列（异步发送）
                    MultiMinuteAggDataDTO msgDTO = new MultiMinuteAggDataDTO();
                    msgDTO.setCopFlag(copFlag);
                    msgDTO.setMinuteAggregateDataDTOList(BeanUtils.toBean(batch, MinuteAggregateDataDTO.class));

                    boolean offered = mqQueue.offer(msgDTO, 5, TimeUnit.SECONDS);
                    if (!offered) {
                        log.warn("【分钟聚合】MQ消息队列已满，消息被丢弃！data:{}", JSONUtil.toJsonStr(msgDTO));
                    }

                } catch (Exception e) {
                    log.error("【分钟聚合】批次处理失败,data:{}", e);
                    throw e;
                }
                return null;
            }));
        }

        // 4. 等待所有线程完成（可加超时）
        for (Future<Void> future : futures) {
            try {
                future.get(); // 也可以加 future.get(60, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.error("多线程处理异常", e);
                throw new IOException("sendMsgToUsageCostBatch 多线程失败", e);
            }
        }

        executor.shutdown();
    }

    @Override
    public void insertSteadyAggDataBatch(List<MinuteAggregateDataDO> aggDataList) throws IOException {
        if (CollUtil.isEmpty(aggDataList)) {
            return;
        }
        List<List<MinuteAggregateDataDO>> batchList = CollUtil.split(aggDataList, batchSize);
        for (List<MinuteAggregateDataDO> batch : batchList) {
            // 执行你的批量插入操作，比如：
            String labelName = System.currentTimeMillis() + STREAM_LOAD_PREFIX + RandomUtil.randomNumbers(6);
            starRocksStreamLoadService.streamLoadData(batch, labelName, MINUTE_AGGREGATE_DATA_TB_NAME);
        }
    }

    @Override
    public void sendMsgToUsageCostBatch(List<MinuteAggregateDataDO> aggDataList, Boolean copFlag) throws IOException {
        if (CollUtil.isEmpty(aggDataList)) {
            return;
        }

        List<List<MinuteAggregateDataDO>> batchList = getHourGroupBatch(aggDataList);
        int poolSize = Math.min(4, Runtime.getRuntime().availableProcessors());
        ExecutorService executor = Executors.newFixedThreadPool(poolSize);
        List<Future<Void>> futures = new ArrayList<>();

        for (List<MinuteAggregateDataDO> batch : batchList) {
            futures.add(executor.submit(() -> {
                try {
                    String labelName = System.currentTimeMillis() + STREAM_LOAD_PREFIX + RandomUtil.randomNumbers(6);
                    starRocksStreamLoadService.streamLoadData(batch, labelName, MINUTE_AGGREGATE_DATA_TB_NAME);

                    // 发送 MQ 消息
                    String topicName = deviceAggTopic;

                    MultiMinuteAggDataDTO msgDTO = new MultiMinuteAggDataDTO();
                    msgDTO.setCopFlag(copFlag);
                    msgDTO.setMinuteAggregateDataDTOList(BeanUtils.toBean(batch, MinuteAggregateDataDTO.class));
                    Message<MultiMinuteAggDataDTO> msg =
                            MessageBuilder.withPayload(msgDTO).build();
                    rocketMQTemplate.send(topicName, msg);
                    log.info("成本计算消息发送，msg header【{}】", msg.getHeaders());
                } catch (Exception e) {
                    log.error("sendMsgToUsageCostBatch: 批次处理失败", e);
                    throw e;
                }
                return null;
            }));
        }

        for (Future<Void> future : futures) {
            try {
                future.get(); // 等待所有线程完成，可加超时 future.get(60, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.error("sendMsgToUsageCostBatch: 多线程批量处理异常", e);
                throw new IOException("sendMsgToUsageCostBatch 多线程处理失败", e);
            }
        }
        executor.shutdown();
    }

    /**
     * 台账+小时粒度 分批次数据
     *
     * @param aggDataList
     * @return
     */
    private List<List<MinuteAggregateDataDO>> getHourGroupBatch(List<MinuteAggregateDataDO> aggDataList) {
        // 1. 按 台账 + 整点小时 分组
        Map<String, List<MinuteAggregateDataDO>> groupMap = aggDataList.stream()
                .collect(Collectors.groupingBy(data -> {
                    Long sbId = data.getStandingbookId();
                    LocalDateTime hour = data.getAggregateTime().withMinute(0).withSecond(0).withNano(0);
                    return sbId + StringPool.UNDERSCORE + hour;
                }));

        // 2. 转成分组列表，并按照 "小时时间" 升序排序
        List<Map.Entry<String, List<MinuteAggregateDataDO>>> sortedGroups = groupMap.entrySet()
                .stream()
                .sorted(Comparator.comparing(entry -> entry.getValue().get(0).getAggregateTime().withMinute(0).withSecond(0).withNano(0)))
                .collect(Collectors.toList());
        List<List<MinuteAggregateDataDO>> finalBatches = new ArrayList<>();
        List<MinuteAggregateDataDO> currentBatch = new ArrayList<>();

        for (Map.Entry<String, List<MinuteAggregateDataDO>> entry : sortedGroups) {
            List<MinuteAggregateDataDO> group = entry.getValue();

            // 判断是否超出最大批次
            if (currentBatch.size() + group.size() > batchSize) {
                finalBatches.add(new ArrayList<>(currentBatch));
                currentBatch.clear();
            }

            currentBatch.addAll(group);
        }

        // 最后一批加入
        if (!currentBatch.isEmpty()) {
            finalBatches.add(currentBatch);
        }
        return finalBatches;
    }

    @Override
    @TenantIgnore
    public void insertDataBatch(List<MinuteAggregateDataDTO> minuteAggregateDataDTO) {
        try {
            if (Objects.isNull(minuteAggregateDataDTO)) {
                return;
            }
            List<MinuteAggregateDataDO> minuteAggregateDataDOList = BeanUtils.toBean(minuteAggregateDataDTO,
                    MinuteAggregateDataDO.class);
            LocalDateTime maxTime = minuteAggregateDataDOList.stream()
                    .map(MinuteAggregateDataDO::getAggregateTime)
                    .filter(Objects::nonNull)
                    .max(Comparator.naturalOrder())
                    .orElse(null);
            LocalDateTime minTime = minuteAggregateDataDOList.stream()
                    .map(MinuteAggregateDataDO::getAggregateTime)
                    .filter(Objects::nonNull)
                    .min(Comparator.naturalOrder())
                    .orElse(null);
            // 先进行业务点的分区建设
            // 检查并创建数据表分区
            partitionService.ensurePartitionsExist(minTime, maxTime);
            // 发送给usageCost进行计算
            sendMsgToUsageCostBatchNew(minuteAggregateDataDOList, true);
        } catch (Exception e) {
            log.error("insertDataBatch失败：{}", e.getMessage(), e);
            throw exception(STREAM_LOAD_RANGE_FAIL);
        }
    }

    @Override
    public List<MinuteAggregateDataDTO> getCopRangeData(List<Long> standingbookIds, List<String> paramCodes, LocalDateTime starTime, LocalDateTime endTime) {
        List<MinuteAggregateDataDO> minuteAggregateDataDOS =
                minuteAggregateDataMapper.getCopRangeData(standingbookIds, paramCodes, starTime, endTime);
        if (CollUtil.isEmpty(minuteAggregateDataDOS)) {
            return null;
        }
        return BeanUtils.toBean(minuteAggregateDataDOS, MinuteAggregateDataDTO.class);
    }

    @Override
    public List<MinuteAggregateDataDTO> getCopRangeDataSteady(List<Long> standingbookIds, List<String> paramCodes, LocalDateTime starTime, LocalDateTime endTime) {
        List<MinuteAggregateDataDO> minuteAggregateDataDOS =
                minuteAggregateDataMapper.getCopRangeDataSteady(standingbookIds, paramCodes, starTime, endTime);
        if (CollUtil.isEmpty(minuteAggregateDataDOS)) {
            return null;
        }
        return BeanUtils.toBean(minuteAggregateDataDOS, MinuteAggregateDataDTO.class);
    }

    @Override
    public MinuteAggregateDataDTO getUsagePrevFullValue(Long standingbookId, LocalDateTime acquisitionTime) {
        MinuteAggregateDataDO minuteAggregateDataDO =
                minuteAggregateDataMapper.getUsagePrevFullValue(standingbookId, acquisitionTime);
        if (Objects.isNull(minuteAggregateDataDO)) {
            return null;
        }
        return BeanUtils.toBean(minuteAggregateDataDO, MinuteAggregateDataDTO.class);
    }

    @Override
    public MinuteAggregateDataDTO getUsageNextFullValue(Long standingbookId, LocalDateTime acquisitionTime) {
        MinuteAggregateDataDO minuteAggregateDataDO =
                minuteAggregateDataMapper.getUsageNextFullValue(standingbookId, acquisitionTime);
        if (Objects.isNull(minuteAggregateDataDO)) {
            return null;
        }
        return BeanUtils.toBean(minuteAggregateDataDO, MinuteAggregateDataDTO.class);
    }

    @Override
    public MinuteAggregateDataDTO getUsageExistFullValue(Long standingbookId, LocalDateTime acquisitionTime) {
        MinuteAggregateDataDO minuteAggregateDataDO =
                minuteAggregateDataMapper.getUsageExistFullValue(standingbookId, acquisitionTime);
        if (Objects.isNull(minuteAggregateDataDO)) {
            return null;
        }
        return BeanUtils.toBean(minuteAggregateDataDO, MinuteAggregateDataDTO.class);
    }

    @Override
    public Map<Long, MinuteAggDataSplitDTO> getPreAndNextData(MinuteRangeDataParamDTO paramDTO) {
        if (CollUtil.isEmpty(paramDTO.getSbIds())) {
            return Collections.emptyMap();
        }
        // 查询多个台账id对应用量的 某时间点的上一条数据
        Map<Long, MinuteAggDataSplitDTO> resultMap = new HashMap<>();

        // 1. 查询前一条数据（按台账 ID）
        List<MinuteAggregateDataDO> preDOs = minuteAggregateDataMapper.getSbIdsUsagePrevFullValue(paramDTO.getSbIds(), paramDTO.getStarTime());
        List<MinuteAggregateDataDTO> preDTOList = CollUtil.isNotEmpty(preDOs) ? BeanUtils.toBean(preDOs, MinuteAggregateDataDTO.class) : Collections.emptyList();

        // 2. 查询后一条数据（按台账 ID）
        List<MinuteAggregateDataDO> nextDOs = minuteAggregateDataMapper.getSbIdsUsageNextFullValue(paramDTO.getSbIds(), paramDTO.getEndTime());
        List<MinuteAggregateDataDTO> nextDTOList = CollUtil.isNotEmpty(nextDOs) ? BeanUtils.toBean(nextDOs, MinuteAggregateDataDTO.class) : Collections.emptyList();

        // 3. 整理成 Map
        Map<Long, MinuteAggregateDataDTO> preMap = preDTOList.stream()
                .collect(Collectors.toMap(MinuteAggregateDataDTO::getStandingbookId, Function.identity(), (v1, v2) -> v1));
        Map<Long, MinuteAggregateDataDTO> nextMap = nextDTOList.stream()
                .collect(Collectors.toMap(MinuteAggregateDataDTO::getStandingbookId, Function.identity(), (v1, v2) -> v1));

        // 4. 封装结果
        for (Long sbId : paramDTO.getSbIds()) {
            MinuteAggregateDataDTO preDTO = preMap.get(sbId);
            MinuteAggregateDataDTO nextDTO = nextMap.get(sbId);
            if (preDTO != null || nextDTO != null) {
                MinuteAggDataSplitDTO splitDTO = new MinuteAggDataSplitDTO();
                splitDTO.setStartDataDO(preDTO);
                splitDTO.setEndDataDO(nextDTO);
                resultMap.put(sbId, splitDTO);
            }
        }

        return resultMap;
    }
}
