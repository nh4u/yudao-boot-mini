package cn.bitlinks.ems.module.acquisition.service.minuteaggregatedata;

import cn.bitlinks.ems.framework.common.enums.AcqFlagEnum;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.framework.tenant.core.aop.TenantIgnore;
import cn.bitlinks.ems.module.acquisition.api.collectrawdata.dto.MinuteAggDataSplitDTO;
import cn.bitlinks.ems.module.acquisition.api.collectrawdata.dto.MinuteAggregateDataDTO;
import cn.bitlinks.ems.module.acquisition.api.minuteaggregatedata.dto.MinuteRangeDataParamDTO;
import cn.bitlinks.ems.module.acquisition.dal.dataobject.minuteaggregatedata.MinuteAggregateDataDO;
import cn.bitlinks.ems.module.acquisition.dal.mysql.minuteaggregatedata.MinuteAggregateDataMapper;
import cn.bitlinks.ems.module.acquisition.service.partition.PartitionService;
import cn.bitlinks.ems.module.acquisition.starrocks.StarRocksStreamLoadService;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.bitlinks.ems.module.acquisition.enums.CommonConstants.*;
import static cn.bitlinks.ems.module.acquisition.enums.ErrorCodeConstants.*;

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

    @Resource
    private PartitionService partitionService;

    @Override
    @TenantIgnore
    public MinuteAggregateDataDTO selectByAggTime(Long standingbookId, LocalDateTime thisCollectTime) {
        MinuteAggregateDataDO minuteAggregateDataDO = minuteAggregateDataMapper.selectExactData(standingbookId,
                thisCollectTime);
        if (Objects.isNull(minuteAggregateDataDO)) {
            return null;
        }
        return BeanUtils.toBean(minuteAggregateDataDO, MinuteAggregateDataDTO.class);
    }


    @Override
    @TenantIgnore
    public MinuteAggregateDataDTO selectOldestByStandingBookId(Long standingbookId) {
        MinuteAggregateDataDO minuteAggregateDataDO =
                minuteAggregateDataMapper.selectOldestByStandingBookId(standingbookId);
        if (Objects.isNull(minuteAggregateDataDO)) {
            return null;
        }
        return BeanUtils.toBean(minuteAggregateDataDO, MinuteAggregateDataDTO.class);
    }

    @Override
    @TenantIgnore
    public MinuteAggregateDataDTO selectLatestByStandingBookId(Long standingbookId) {
        MinuteAggregateDataDO minuteAggregateDataDO =
                minuteAggregateDataMapper.selectLatestByStandingBookId(standingbookId);
        if (Objects.isNull(minuteAggregateDataDO)) {
            return null;
        }
        return BeanUtils.toBean(minuteAggregateDataDO, MinuteAggregateDataDTO.class);
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
//            // 发送mq消息
//            String topicName = deviceSteadyAggTopic;
//            // 发送消息
//            Message<List<MinuteAggregateDataDTO>> msg =
//                    MessageBuilder.withPayload(BeanUtils.toBean(batch, MinuteAggregateDataDTO.class)).build();
//            rocketMQTemplate.send(topicName, msg);
        }
    }

    public void sendMsgToUsageCostBatchOld(List<MinuteAggregateDataDO> aggDataList) throws IOException {
        if (CollUtil.isEmpty(aggDataList)) {
            return;
        }
        List<List<MinuteAggregateDataDO>> batchList = getHourGroupBatch(aggDataList);

        for (List<MinuteAggregateDataDO> batch : batchList) {
            // 执行你的批量插入操作，比如：
            String labelName = System.currentTimeMillis() + STREAM_LOAD_PREFIX + RandomUtil.randomNumbers(6);
            starRocksStreamLoadService.streamLoadData(batch, labelName, MINUTE_AGGREGATE_DATA_TB_NAME);
            // 发送mq消息
            String topicName = deviceAggTopic;
            // 发送消息
            Message<List<MinuteAggregateDataDTO>> msg =
                    MessageBuilder.withPayload(BeanUtils.toBean(batch, MinuteAggregateDataDTO.class)).build();
            rocketMQTemplate.send(topicName, msg);
        }
    }

    @Override
    public void sendMsgToUsageCostBatch(List<MinuteAggregateDataDO> aggDataList) throws IOException {
        if (CollUtil.isEmpty(aggDataList)) {
            return;
        }

        List<List<MinuteAggregateDataDO>> batchList = getHourGroupBatch(aggDataList);
        int poolSize = Math.min(batchList.size(), Runtime.getRuntime().availableProcessors() * 2);
        ExecutorService executor = Executors.newFixedThreadPool(poolSize);
        List<Future<Void>> futures = new ArrayList<>();

        for (List<MinuteAggregateDataDO> batch : batchList) {
            futures.add(executor.submit(() -> {
                try {
                    String labelName = System.currentTimeMillis() + STREAM_LOAD_PREFIX + RandomUtil.randomNumbers(6);
                    starRocksStreamLoadService.streamLoadData(batch, labelName, MINUTE_AGGREGATE_DATA_TB_NAME);

                    // 发送 MQ 消息
                    String topicName = deviceAggTopic;
                    Message<List<MinuteAggregateDataDTO>> msg =
                            MessageBuilder.withPayload(BeanUtils.toBean(batch, MinuteAggregateDataDTO.class)).build();
                    rocketMQTemplate.send(topicName, msg);
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
    @Transactional
    public void insertSingleData(MinuteAggregateDataDTO minuteAggregateDataDTO) {
        try {
            if (Objects.isNull(minuteAggregateDataDTO)) {
                return;
            }
            MinuteAggregateDataDO minuteAggregateDataDO = BeanUtils.toBean(minuteAggregateDataDTO,
                    MinuteAggregateDataDO.class);
            // 创建聚合数据表分区
            partitionService.createPartitions(MINUTE_AGGREGATE_DATA_TB_NAME, minuteAggregateDataDO.getAggregateTime(), minuteAggregateDataDO.getAggregateTime());
            // 创建聚合数据计算表分区
            partitionService.createPartitions(USAGE_COST_TB_NAME, minuteAggregateDataDO.getAggregateTime(), minuteAggregateDataDO.getAggregateTime());
            // 创建聚合数据计算表分区
            partitionService.createPartitions(COP_HOUR_AGGREGATE_DATA_TB_NAME, minuteAggregateDataDO.getAggregateTime(), minuteAggregateDataDO.getAggregateTime());
            // 发送给usageCost进行计算
            sendMsgToUsageCostBatch(Collections.singletonList(minuteAggregateDataDO));
        } catch (Exception e) {
            log.error("insertSingleData失败：{}", e.getMessage(), e);
            throw exception(STREAM_LOAD_SINGLE_FAIL);
        }
    }

    @Override
    @TenantIgnore
    public void insertRangeData(MinuteAggDataSplitDTO minuteAggDataSplitDTO) {
        try {
            List<MinuteAggregateDataDO> minuteAggregateDataDOS = splitData(minuteAggDataSplitDTO.getStartDataDO(),
                    minuteAggDataSplitDTO.getEndDataDO());
            if (CollUtil.isEmpty(minuteAggregateDataDOS)) {
                return;
            }
            // 创建聚合数据表分区
            partitionService.createPartitions(MINUTE_AGGREGATE_DATA_TB_NAME, minuteAggregateDataDOS.get(0).getAggregateTime(), minuteAggregateDataDOS.get(minuteAggregateDataDOS.size() - 1).getAggregateTime());
            // 创建聚合数据计算表分区
            partitionService.createPartitions(USAGE_COST_TB_NAME, minuteAggregateDataDOS.get(0).getAggregateTime(), minuteAggregateDataDOS.get(minuteAggregateDataDOS.size() - 1).getAggregateTime());
            // 创建聚合数据计算表分区
            partitionService.createPartitions(COP_HOUR_AGGREGATE_DATA_TB_NAME, minuteAggregateDataDOS.get(0).getAggregateTime(), minuteAggregateDataDOS.get(minuteAggregateDataDOS.size() - 1).getAggregateTime());
            // 发送给usageCost进行计算
            sendMsgToUsageCostBatch(minuteAggregateDataDOS);
        } catch (Exception e) {
            log.error("insertRangeData失败：{}", e.getMessage(), e);
            throw exception(STREAM_LOAD_RANGE_FAIL);
        }
    }

    @Override
    public List<MinuteAggregateDataDTO> getRangeDataRequestParam(List<Long> standingbookIds, LocalDateTime starTime, LocalDateTime endTime) {
        List<MinuteAggregateDataDO> minuteAggregateDataDOS =
                minuteAggregateDataMapper.getRangeDataRequestParam(standingbookIds, starTime, endTime);
        if (CollUtil.isEmpty(minuteAggregateDataDOS)) {
            return null;
        }
        return BeanUtils.toBean(minuteAggregateDataDOS, MinuteAggregateDataDTO.class);
    }

    /**
     * 根据分钟级的数据进行数据拆分，填充两端时间之间的分钟级别数据，计算出全量和增量值，塞到MinuteAggregateDataDO中
     *
     * @param startData 开始数据
     * @param endData   结束数据
     */
    private List<MinuteAggregateDataDO> splitData(MinuteAggregateDataDTO startData, MinuteAggregateDataDTO endData) {

        LocalDateTime startTime = startData.getAggregateTime();
        LocalDateTime endTime = endData.getAggregateTime();
        BigDecimal startValue = startData.getFullValue();
        BigDecimal endValue = endData.getFullValue();

        List<MinuteAggregateDataDO> result = new ArrayList<>();

        // 计算时间差（分钟）
        long minutes = Duration.between(startTime, endTime).toMinutes();
        if (minutes <= 0) {
            return result; // 无需处理
        }

        // 计算每分钟的增量值
        BigDecimal totalIncrement = endValue.subtract(startValue);
        BigDecimal perMinuteIncrement = totalIncrement.divide(BigDecimal.valueOf(minutes), 10, RoundingMode.HALF_UP);

        // 初始化当前时间和当前全量值
        LocalDateTime currentTime = startTime;
        BigDecimal currentFullValue = startValue;

        for (int i = 0; i <= minutes; i++) {
            MinuteAggregateDataDO data = new MinuteAggregateDataDO();
            data.setStandingbookId(startData.getStandingbookId());
            data.setParamCode(startData.getParamCode());
            data.setEnergyFlag(startData.getEnergyFlag());
            data.setFullIncrement(startData.getFullIncrement());
            data.setDataSite(startData.getDataSite());
            data.setDataFeature(startData.getDataFeature());
            data.setDataType(startData.getDataType());
            data.setUsage(startData.getUsage());
            data.setAcqFlag(AcqFlagEnum.NOT_ACQ.getCode());


            data.setAggregateTime(currentTime);
            data.setFullValue(currentFullValue);
            data.setIncrementalValue(perMinuteIncrement);
            if (i == 0) {
                if (Objects.isNull(endData.getIncrementalValue())) {
                    //这个是历史时间段之后添加的连续数据，两个时间点全量都有，需要计算出最后一个时间点的增量和，时间范围之间的分钟级数据的增量
                    //第一条数据的值，还是第一条数据的值，不需要加入新增的队列中
                    data.setIncrementalValue(startData.getIncrementalValue());
                    // 更新当前时间和全量值
                    currentTime = currentTime.plusMinutes(1);
                    currentFullValue = currentFullValue.add(perMinuteIncrement);
                    continue;
                }
                if (endData.getIncrementalValue().equals(BigDecimal.ZERO)) {
                    //这个是历史时间段之前添加的连续数据，都是全量，第一个时间点的增量为0不需要动，需要计算出最后一个时间点的增量和时间范围之间的分钟级别数据的增量
                    data.setIncrementalValue(BigDecimal.ZERO);
                }
                if (startData.getAcqFlag() != null) {
                    data.setAcqFlag(startData.getAcqFlag());
                } else {
                    data.setAcqFlag(AcqFlagEnum.NOT_ACQ.getCode());
                }
            }
            if (i == minutes) {
                data.setAcqFlag(AcqFlagEnum.ACQ.getCode());
                if (endData.getAcqFlag() != null) {
                    data.setAcqFlag(endData.getAcqFlag());
                } else {
                    data.setAcqFlag(AcqFlagEnum.NOT_ACQ.getCode());
                }
                data.setFullValue(endValue);
            }

            // 如果开始时间为空，按
            result.add(data);
            // 更新当前时间和全量值
            currentTime = currentTime.plusMinutes(1);
            currentFullValue = currentFullValue.add(perMinuteIncrement);
        }

        return result;
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
    public Map<Long, MinuteAggDataSplitDTO> getPreAndNextData( MinuteRangeDataParamDTO paramDTO) {
        if(CollUtil.isEmpty(paramDTO.getSbIds())){
            return Collections.emptyMap();
        }
        // 查询多个台账id对应用量的 某时间点的上一条数据
        Map<Long, MinuteAggDataSplitDTO> resultMap = new HashMap<>();

        // 1. 查询前一条数据（按台账 ID）
        List<MinuteAggregateDataDO> preDOs = minuteAggregateDataMapper.getSbIdsUsagePrevFullValue(paramDTO.getSbIds(), paramDTO.getStarTime());
        List<MinuteAggregateDataDTO> preDTOList = CollUtil.isNotEmpty(preDOs) ? BeanUtils.toBean(preDOs, MinuteAggregateDataDTO.class) : Collections.emptyList();

        // 2. 查询后一条数据（按台账 ID）
        List<MinuteAggregateDataDO> nextDOs = minuteAggregateDataMapper.getSbIdsUsageNextFullValue(paramDTO.getSbIds(), paramDTO.getStarTime());
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
