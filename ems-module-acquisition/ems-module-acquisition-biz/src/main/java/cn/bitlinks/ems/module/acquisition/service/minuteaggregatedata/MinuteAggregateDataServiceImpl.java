package cn.bitlinks.ems.module.acquisition.service.minuteaggregatedata;

import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.framework.tenant.core.aop.TenantIgnore;
import cn.bitlinks.ems.module.acquisition.api.collectrawdata.dto.MinuteAggDataSplitDTO;
import cn.bitlinks.ems.module.acquisition.api.collectrawdata.dto.MinuteAggregateDataDTO;
import cn.bitlinks.ems.module.acquisition.api.collectrawdata.dto.MultiMinuteAggDataDTO;
import cn.bitlinks.ems.module.acquisition.api.minuteaggregatedata.dto.MinuteRangeDataParamDTO;
import cn.bitlinks.ems.module.acquisition.dal.dataobject.minuteaggregatedata.MinuteAggregateDataDO;
import cn.bitlinks.ems.module.acquisition.dal.mysql.minuteaggregatedata.MinuteAggregateDataMapper;
import cn.bitlinks.ems.module.acquisition.starrocks.StarRocksStreamLoadService;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
    @Resource(name = "starRocksAsyncExecutor")
    private ExecutorService starRocksAsyncExecutor;

    @Resource
    private RocketMQTemplate rocketMQTemplate;
    @Value("${rocketmq.topic.device-aggregate}")
    private String deviceAggTopic;

    /**
     * MQ分发：按原有逻辑拆分（比如按小时+100条/批）
     */
    private void dispatchToMQ(List<MinuteAggregateDataDO> aggDataList, Boolean copFlag) {
        // 1. 按原有逻辑拆分（比如和之前的getHourGroupBatch逻辑一致，按小时+100条拆分）
        List<List<MinuteAggregateDataDO>> batchList = getHourGroupBatch(aggDataList);
        // 2. 对每个MQ小批，构造DTO并发送到MQ
        for (List<MinuteAggregateDataDO> batch : batchList) {
            // 3. 构造 MQ DTO 并加入队列（异步发送）
            MultiMinuteAggDataDTO msgDTO = new MultiMinuteAggDataDTO();
            msgDTO.setCopFlag(copFlag);
            msgDTO.setMinuteAggregateDataDTOList(BeanUtils.toBean(batch, MinuteAggregateDataDTO.class));

            Message<MultiMinuteAggDataDTO> msg = MessageBuilder.withPayload(msgDTO).build();
            rocketMQTemplate.send(deviceAggTopic, msg);
        }
    }

    /**
     * 主要用于手动补录、批量补录数据分钟级数据处理  然后保存到usage_cost表中
     *
     * @param aggDataList
     * @param copFlag
     * @throws IOException
     */
    @TenantIgnore
    public void sendMsgToUsageCostBatchNew(List<MinuteAggregateDataDO> aggDataList, Boolean copFlag) throws IOException {
        if (CollUtil.isEmpty(aggDataList)) {
            return;
        }
        // 复制copFlag到局部变量，避免跨线程引用风险
        final Boolean finalCopFlag = copFlag != null ? copFlag : false;
        // StarRocks 导入批次（1万条/批）
        List<List<MinuteAggregateDataDO>> starRocksBatches = Lists.partition(aggDataList, STAR_ROCKS_BATCH_SIZE);
        log.info("StarRocks 导入批次数量：{}，总数据量：{}", starRocksBatches.size(), aggDataList.size());
        // 异步执行StarRocks导入（1w条/批，调用次数极少）
        for (int i = 0; i < starRocksBatches.size(); i++) {
            List<MinuteAggregateDataDO> srBatch = starRocksBatches.get(i);
            int batchIndex = i; // 批次索引，用于label唯一化
            // 异步执行 StarRocks 导入 + MQ 分发
            starRocksAsyncExecutor.submit(() -> {
                try {
                    // 1. StarRocks 批量导入
                    String labelName = batchIndex + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
                    starRocksStreamLoadService.streamLoadData(srBatch, labelName, MINUTE_AGGREGATE_DATA_TB_NAME);
                    log.info("StarRocks 导入成功，批次数据量：{}，label：{}", srBatch.size(), labelName);
                    // 第二步：MQ 分发
                    dispatchToMQ(srBatch, finalCopFlag);
                } catch (Exception e) {
                    log.error("StarRocks 导入或 MQ 分发失败，批次数据量：{}", srBatch.size(), e);
                }
            });
        }
    }
    @TenantIgnore
    public void sendMsgToUsageCostBatchNewTest(List<MinuteAggregateDataDO> aggDataList, Boolean copFlag) throws IOException {
        if (CollUtil.isEmpty(aggDataList)) {
            return;
        }
        // 复制copFlag到局部变量，避免跨线程引用风险
        final Boolean finalCopFlag = copFlag != null ? copFlag : false;
        // StarRocks 导入批次（1万条/批）
        List<List<MinuteAggregateDataDO>> starRocksBatches = Lists.partition(aggDataList, STAR_ROCKS_BATCH_SIZE);
        log.info("StarRocks 导入批次数量：{}，总数据量：{}", starRocksBatches.size(), aggDataList.size());
        // 异步执行StarRocks导入（1w条/批，调用次数极少）
        for (int i = 0; i < starRocksBatches.size(); i++) {
            List<MinuteAggregateDataDO> srBatch = starRocksBatches.get(i);
            int batchIndex = i; // 批次索引，用于label唯一化
            // 异步执行 StarRocks 导入 + MQ 分发
            starRocksAsyncExecutor.submit(() -> {
                try {
                    // 1. StarRocks 批量导入
                    String labelName = batchIndex + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
                    starRocksStreamLoadService.streamLoadData(srBatch, labelName, MINUTE_AGGREGATE_DATA_TB_NAME);
                    log.info("StarRocks 导入成功，批次数据量：{}，label：{}", srBatch.size(), labelName);
                } catch (Exception e) {
                    log.error("StarRocks 导入或 MQ 分发失败，批次数据量：{}", srBatch.size(), e);
                }
            });
        }
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

    /**
     * 主要用于 数采实时数据分钟聚合数据处理   聚合数据调用的方法 用于把分钟级别数据发送到mq中 然后保存到usage_cost表中
     *
     * @param aggDataList
     * @param copFlag
     * @throws IOException
     */
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
            if (currentBatch.size() + group.size() > 100) {
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
            // 发送给usageCost进行计算
            sendMsgToUsageCostBatchNew(minuteAggregateDataDOList, true);
        } catch (Exception e) {
            log.error("insertDataBatch失败：{}", e.getMessage(), e);
            throw exception(STREAM_LOAD_RANGE_FAIL);
        }
    }

    @Override
    @TenantIgnore
    public void insertDataBatchTest(List<MinuteAggregateDataDTO> minuteAggregateDataDTO) {
        try {
            if (Objects.isNull(minuteAggregateDataDTO)) {
                return;
            }
            List<MinuteAggregateDataDO> minuteAggregateDataDOList = BeanUtils.toBean(minuteAggregateDataDTO,
                    MinuteAggregateDataDO.class);
            // 发送给usageCost进行计算
            sendMsgToUsageCostBatchNewTest(minuteAggregateDataDOList, true);
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
